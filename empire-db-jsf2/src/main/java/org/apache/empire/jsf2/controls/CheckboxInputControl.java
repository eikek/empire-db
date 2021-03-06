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
package org.apache.empire.jsf2.controls;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.InternalException;
import org.apache.empire.jsf2.controls.InputControl;

public class CheckboxInputControl extends InputControl
{
    public static final String NAME = "checkbox";

    private Class<? extends javax.faces.component.html.HtmlSelectBooleanCheckbox> inputComponentClass;

    public CheckboxInputControl(Class<? extends HtmlSelectBooleanCheckbox> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public CheckboxInputControl()
    {
        this(javax.faces.component.html.HtmlSelectBooleanCheckbox.class);
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlSelectBooleanCheckbox input;
        try
        {   input = inputComponentClass.newInstance();
		} catch (InstantiationException e1) {
			throw new InternalException(e1);
		} catch (IllegalAccessException e2) {
			throw new InternalException(e2);
		}
        copyAttributes(parent, ii, input);
        
        setInputValue(input, ii);
        input.setDisabled(ii.isDisabled());
        
        compList.add(input);
    }

}
