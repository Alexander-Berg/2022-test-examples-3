package ru.yandex.market.crm.triggers.test.helpers.builders;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;

public class CrmProcessBuilder extends AbstractProcessBuilder<CrmProcessBuilder> {

    public CrmProcessBuilder(BpmnModelInstance modelInstance, Process process) {
        super(modelInstance, process, CrmProcessBuilder.class);
    }

    public CrmStartEventBuilder startEvent() {
        return startEvent(null);
    }

    public CrmStartEventBuilder startEvent(String id) {
        StartEvent start = createChild(StartEvent.class, id);
        BpmnShape bpmnShape = createBpmnShape(start);
        setCoordinates(bpmnShape);
        return new CrmStartEventBuilder(modelInstance, start);
    }

    public EventSubProcessBuilder eventSubProcess(){
        return eventSubProcess(null);
    }

    public EventSubProcessBuilder eventSubProcess(String id) {
        // Create a subprocess, triggered by an event, and add it to modelInstance
        SubProcess subProcess = createChild(SubProcess.class, id);
        subProcess.setTriggeredByEvent(true);

        // Create Bpmn shape so subprocess will be drawn
        BpmnShape targetBpmnShape = createBpmnShape(subProcess);
        //find the lowest shape in the process
        // place event sub process underneath
        setEventSubProcessCoordinates(targetBpmnShape);

        resizeSubProcess(targetBpmnShape);

        // Return the eventSubProcessBuilder
        return new EventSubProcessBuilder(modelInstance, subProcess);
    }

    @Override
    protected void setCoordinates(BpmnShape targetBpmnShape) {
        Bounds bounds = targetBpmnShape.getBounds();
        bounds.setX(100);
        bounds.setY(100);
    }

    private void setEventSubProcessCoordinates(BpmnShape targetBpmnShape) {
        Bounds targetBounds = targetBpmnShape.getBounds();
        double lowestheight = 0;

        // find the lowest element in the model
        Collection<BpmnShape> allShapes = modelInstance.getModelElementsByType(BpmnShape.class);
        for (BpmnShape shape : allShapes) {
            Bounds bounds = shape.getBounds();
            double bottom = bounds.getY() + bounds.getHeight();
            if(bottom > lowestheight) {
                lowestheight = bottom;
            }
        }

        // move target
        targetBounds.setY(lowestheight + 50.0);
        targetBounds.setX(100.0);
    }
}
