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
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextAreaFieldRenderer extends FieldRenderer
{
    private static final Logger log = LoggerFactory.getLogger(TextAreaFieldRenderer.class);
    
    public static final String NAME = "textarea"; 
    
    private Class<? extends javax.faces.component.html.HtmlInputTextarea> inputComponentClass;

    public TextAreaFieldRenderer(Class<? extends HtmlInputTextarea> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public TextAreaFieldRenderer()
    {
        this(javax.faces.component.html.HtmlInputTextarea.class);
    }
    
    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlInputTextarea input;
		try {
			input = inputComponentClass.newInstance();
		} catch (InstantiationException e1) {
			throw new InternalException(e1);
		} catch (IllegalAccessException e2) {
			throw new InternalException(e2);
		}
        copyAttributes(parent, ii, input);
        
        int cols = ii.getHSize();
        if (cols>0)
            input.setCols(cols);

        int rows = ii.getVSize();
        if (rows>0)
            input.setRows(rows);
        
        input.setDisabled(ii.isDisabled());
        input.setValue(ii.getValue());
        
        compList.add(input);
    }
    
}