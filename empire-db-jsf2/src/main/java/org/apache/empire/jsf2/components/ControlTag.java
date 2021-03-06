/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.jsf2.components;

import java.io.IOException;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.jsf2.controls.InputControl;
import org.apache.empire.jsf2.utils.TagEncodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlTag extends UIInput implements NamingContainer
{
    public static String DEFAULT_CONTROL_SEPARATOR_TAG = "td";
    public static String DEFAULT_LABEL_SEPARATOR_CLASS = "eCtlLabel";
    public static String DEFAULT_INPUT_SEPARATOR_CLASS = "eCtlInput";
    
    public static abstract class ControlSeparatorComponent extends javax.faces.component.UIComponentBase
    {
        private ControlTag control = null;

        /*
        protected ControlSeparatorComponent()
        {
            if (log.isTraceEnabled())
                log.trace("ControlSeparatorComponent "+getClass().getName()+" created.");
        }
        */

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY; 
        }
        
        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);
            
            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for "+getClass().getName());
                return;
            }
            
            control = (ControlTag)parent;
        }
        
        protected abstract void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName) throws IOException;
        
        @Override
        public boolean getRendersChildren()
        {
            return true;
        }
        
        @Override
        public void encodeChildren(FacesContext context)
            throws IOException
        {
            if (control!=null)
            {   // write end tag
                TagEncodingHelper helper = control.helper;
                String tagName = helper.getTagAttribute("tag", "td");
                
                // render components
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement(tagName, this);
                writeAttributes(writer, helper, tagName);
                // write children
                super.encodeChildren(context);
                // end
                writer.endElement(tagName);
            }
        }
    }

    public static class LabelSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName) 
            throws IOException
        {
            String styleClass = helper.getTagAttribute("labelClass", DEFAULT_LABEL_SEPARATOR_CLASS);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
        }
    }

    public static class InputSeparatorComponent extends ControlSeparatorComponent
    {
        @Override
        protected void writeAttributes(ResponseWriter writer, TagEncodingHelper helper, String tagName) 
            throws IOException
        {
            String styleClass = helper.getTagAttribute("inputClass", DEFAULT_INPUT_SEPARATOR_CLASS);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
            String colSpan = helper.getTagAttribute("colspan");
            if (StringUtils.isNotEmpty(colSpan) && tagName.equalsIgnoreCase("td"))
                writer.writeAttribute("colspan", colSpan, null);
        }
    }
    
    public static class ValueOutputComponent extends javax.faces.component.UIComponentBase
    {
        private final String tagName = "span";

        /*
        public ValueOutputComponent()
        {
            if (log.isTraceEnabled())
                log.trace("ValueOutputComponent created.");
        } 
        */

        @Override
        public String getFamily()
        {
            return UINamingContainer.COMPONENT_FAMILY; 
        }
        
        @Override
        public void encodeBegin(FacesContext context)
            throws IOException
        {
            super.encodeBegin(context);
            
            UIComponent parent = getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
                parent = parent.getParent();
            if (!(parent instanceof ControlTag))
            {   log.error("Invalid parent component for "+getClass().getName());
                return;
            }
            
            ControlTag controlTag = (ControlTag)parent;
            TagEncodingHelper helper = controlTag.helper;

            InputControl control = helper.getInputControl();
            InputControl.ValueInfo valInfo = helper.getValueInfo(context);
            String styleClass = helper.getTagStyleClass("eInpDis");
            String tooltip    = helper.getValueTooltip(helper.getTagAttribute("title"));
            
            // render components
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(tagName, this);
            if (StringUtils.isNotEmpty(styleClass))
                writer.writeAttribute("class", styleClass, null);
            if (StringUtils.isNotEmpty(tooltip))
                writer.writeAttribute("title", tooltip, null);
            // render Value
            control.renderValue(valInfo, writer);
            writer.endElement(tagName);
        }
    }
    
    // Logger
    private static final Logger  log          = LoggerFactory.getLogger(ControlTag.class);

    private static final String readOnlyState  = "readOnlyState";
    
    private static final boolean encodeLabel = true;

    protected final TagEncodingHelper helper = new TagEncodingHelper(this, "eInput");

    protected InputControl control = null;
    protected InputControl.InputInfo inpInfo = null;

    public ControlTag()
    {
        super();
    }

    @Override
    public String getFamily()
    {
        return "javax.faces.NamingContainer";
    }

    private void saveState()
    {
        // getStateHelper().put(inpControlPropName, control);
        // getStateHelper().put(inputInfoPropName, inpInfo);
        getStateHelper().put(readOnlyState, (inpInfo==null));
    }

    private boolean initState(FacesContext context)
    {
        Boolean ros = (Boolean)getStateHelper().get(readOnlyState);
        if (ros!=null && ros.booleanValue())
            return false;
        // control = ;
        control = helper.getInputControl();
        inpInfo = helper.getInputInfo(context);
        return (control!=null && inpInfo!=null);
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // add label and input components when the view is loaded for the first time
        super.encodeBegin(context);
        
        // init
        helper.encodeBegin();
        control = helper.getInputControl();
        
        boolean isCustomInput = isCustomInput();
        
        // create children
        if (encodeLabel)
        {   // Create Label Separator Tag
            ControlSeparatorComponent labelSepTag = null; 
            if (getChildCount() > 0)
                labelSepTag = (ControlSeparatorComponent) getChildren().get(0);
            if (labelSepTag == null)
            {   labelSepTag = new LabelSeparatorComponent();
                getChildren().add(labelSepTag);
            }
            labelSepTag.setRendered(true);
            encodeLabel(context, labelSepTag);
            if (isCustomInput)
            {   // don't render twice!
                labelSepTag.setRendered(false);
            }    
        }   

        if (!isCustomInput)
        {   // Create Input Separator Tag
            ControlSeparatorComponent inputSepTag = null; 
            if (getChildCount() > 1)
                inputSepTag = (ControlSeparatorComponent) getChildren().get(1);
            if (inputSepTag == null)
            {   inputSepTag = new InputSeparatorComponent();
                getChildren().add(inputSepTag);
            }
            encodeInput(context, inputSepTag);
        }
        
        /*
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("td", this);
        writer.write("hello world!");
        writer.endElement("td");

        if (!isCustomInput())
        {
            writer.startElement("td", this);
            encodeInput(context, this);
            writer.endElement("td");
        }
        */
        
        saveState();
    }
    
    @Override
    public boolean getRendersChildren()
    {
        return true;
    }
    
    @Override 
    public void encodeChildren(FacesContext context) 
        throws IOException 
    {
        if (isCustomInput())
        {
            String tagName  = helper.getTagAttribute("tag", DEFAULT_CONTROL_SEPARATOR_TAG);
            String inpClass = helper.getTagAttribute("inputClass", DEFAULT_INPUT_SEPARATOR_CLASS);
            String colSpan  = helper.getTagAttribute("colspan");

            ResponseWriter writer = context.getResponseWriter();
            writer.startElement(tagName, this);
            if (StringUtils.isNotEmpty(inpClass))
                writer.writeAttribute("class", inpClass, null);
            if (StringUtils.isNotEmpty(colSpan) && tagName.equalsIgnoreCase("td"))
                writer.writeAttribute("colspan", colSpan, null);
            // encode children
            super.encodeChildren(context);
            // end of element
            writer.endElement(tagName);
        }    
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException 
    {
        super.encodeEnd(context);
    }
    
    private boolean isCustomInput()
    {
        Object custom = getAttributes().get("custom");
        if (custom != null)
            return ObjectUtils.getBoolean(custom);
        return false;
    }
    
    private void encodeLabel(FacesContext context, UIComponentBase parent)
        throws IOException
    {
        // render components
        HtmlOutputLabel labelComponent = null;
        if (parent.getChildCount() > 0)
        {
            labelComponent = (HtmlOutputLabel) parent.getChildren().get(0);
        }
        if (labelComponent == null)
        {
            String forInput = isCustomInput() ? helper.getTagAttribute("for") : "*";
            // createLabelComponent 
            labelComponent = helper.createLabelComponent(context, forInput, "eLabel", null, true);
            parent.getChildren().add(0, labelComponent);
        }
        // render components
        parent.encodeAll(context);
    }
    
    private void encodeInput(FacesContext context, UIComponentBase parent)
        throws IOException
    {
        // render components
        if (helper.isRecordReadOnly())
        {
            ValueOutputComponent valueComp = null;
            if (parent.getChildCount()>0)
                valueComp = (ValueOutputComponent)parent.getChildren().get(0);
            if (valueComp==null)
            {   valueComp = new ValueOutputComponent();
                parent.getChildren().add(valueComp);
            }
        }
        else
        {
            inpInfo = helper.getInputInfo(context);
            // render input
            control.renderInput(parent, inpInfo, context, false);
        }
        // render components
        parent.encodeAll(context);
    }

    @Override
    public Object getValue()
    {
        // check for record
        if (helper.getRecord()!=null)
            return helper.getDataValue(true);
        // default
        Object value = super.getValue();
        return value; 
    }

    @Override
    public Object getSubmittedValue()
    {   // Check state
        if (control==null || inpInfo==null)
            return null;
        // Get Input Tag
        if (getChildCount()<=1)
            return null;
        ControlSeparatorComponent inputSepTag = (ControlSeparatorComponent) getChildren().get(1); 
        // get Input Value
        return control.getInputValue(inputSepTag, inpInfo, true);
    }

    @Override
    public void validateValue(FacesContext context, Object value)
    {   // Check state
        if (inpInfo==null)
            return;
        // Validate value
        inpInfo.validate(value);
        setValid(true);
        // call base class 
        super.validateValue(context, value);
    }    
    
    @Override
    public void validate(FacesContext context)
    {
        if (initState(context)==false)
            return;
        // check disabled
        if (inpInfo.isDisabled())
            return;
        // get submitted value and validate
        if (log.isDebugEnabled())
            log.debug("Validating input for {}.", inpInfo.getColumn().getName());
        
        // Validate value
        try {
            // Will internally call getSubmittedValue() and validateValue() 
            super.validate(context);
            
        } catch(Exception e) {
            // Value is not valid
            helper.addErrorMessage(context, e);
            setValid(false);
        }
    }

    @Override
    public void updateModel(FacesContext context)
    {
        if (initState(context)==false)
            return;
        // check disabled
        if (inpInfo.isDisabled())
            return;
        // No Action
        if (!isValid() || !isLocalValueSet())
            return; 
        // super.updateModel(context);
        log.debug("Updating model input for {}.", inpInfo.getColumn().getName());
        inpInfo.setValue(getLocalValue());
        setValue(null);
        setLocalValueSet(false);
    }
    
    public Column getInputColumn()
    {
        return helper.getColumn();
    }
    
    public boolean isInputReadOnly()
    {
        return helper.isRecordReadOnly();
    }
    
    public boolean isInputRequired()
    {
        return helper.isValueRequired();
    }
}
