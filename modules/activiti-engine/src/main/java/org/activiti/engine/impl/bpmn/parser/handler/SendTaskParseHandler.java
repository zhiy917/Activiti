/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.bpmn.parser.handler;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.DataAssociation;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.SendTask;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.activiti.engine.impl.bpmn.data.AbstractDataAssociation;
import org.activiti.engine.impl.bpmn.data.IOSpecification;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.webservice.Operation;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.apache.commons.lang.StringUtils;


/**
 * @author Joram Barrez
 */
public class SendTaskParseHandler extends AbstractMultiInstanceEnabledParseHandler<SendTask> {
  
  public Class< ? extends BaseElement> getHandledType() {
    return SendTask.class;
  }
  
  protected void executeParse(BpmnParse bpmnParse, SendTask sendTask, ScopeImpl scope, ActivityImpl activityImpl, SubProcess subProcess) {
 
    ActivityImpl activity = createActivityOnScope(bpmnParse, sendTask, BpmnXMLConstants.ELEMENT_TASK_SEND, scope);
    
    activity.setAsync(sendTask.isAsynchronous());
    activity.setExclusive(!sendTask.isNotExclusive());

    // for e-mail
    if (StringUtils.isNotEmpty(sendTask.getType())) {
      if (sendTask.getType().equalsIgnoreCase("mail")) {
        validateFieldDeclarationsForEmail(bpmnParse, sendTask, sendTask.getFieldExtensions());
        activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createMailActivityBehavior(sendTask));
      } else if (sendTask.getType().equalsIgnoreCase("mule")) {
        activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createMuleActivityBehavior(sendTask, bpmnParse.getBpmnModel()));
      } else {
        bpmnParse.getBpmnModel().addProblem("Invalid usage of type attribute: '" + sendTask.getType() + "'.", sendTask);
      }

      // for web service
    } else if (ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE.equalsIgnoreCase(sendTask.getImplementationType()) && 
        StringUtils.isNotEmpty(sendTask.getOperationRef())) {
      
      if (!bpmnParse.getOperations().containsKey(sendTask.getOperationRef())) {
        bpmnParse.getBpmnModel().addProblem(sendTask.getOperationRef() + " does not exist", sendTask);
      } else {
        WebServiceActivityBehavior webServiceActivityBehavior = bpmnParse.getActivityBehaviorFactory().createWebServiceActivityBehavior(sendTask);
        Operation operation = bpmnParse.getOperations().get(sendTask.getOperationRef());
        webServiceActivityBehavior.setOperation(operation);

        if (sendTask.getIoSpecification() != null) {
          IOSpecification ioSpecification = bpmnParse.createIOSpecification(sendTask.getIoSpecification());
          webServiceActivityBehavior.setIoSpecification(ioSpecification);
        }

        for (DataAssociation dataAssociationElement : sendTask.getDataInputAssociations()) {
          AbstractDataAssociation dataAssociation = bpmnParse.createDataInputAssociation(dataAssociationElement);
          webServiceActivityBehavior.addDataInputAssociation(dataAssociation);
        }

        for (DataAssociation dataAssociationElement : sendTask.getDataOutputAssociations()) {
          AbstractDataAssociation dataAssociation = bpmnParse.createDataOutputAssociation(dataAssociationElement);
          webServiceActivityBehavior.addDataOutputAssociation(dataAssociation);
        }

        activity.setActivityBehavior(webServiceActivityBehavior);
      }
    } else {
      bpmnParse.getBpmnModel().addProblem("One of the attributes 'type' or 'operation' is mandatory on sendTask.", sendTask);
    }

    createExecutionListenersOnScope(bpmnParse, sendTask.getExecutionListeners(), activity);
  }

}
