/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.userinput.gui;

import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.IzPanel;
import com.izforge.izpack.panels.userinput.DirInputField;
import com.izforge.izpack.panels.userinput.field.file.DirField;


/**
 * Directory field view.
 *
 * @author Tim Anderson
 */
public class GUIDirFieldView extends AbstractGUIFileFieldView
{

    /**
     * Constructs a {@code GUIDirFieldView}.
     *
     * @param field       the field
     * @param installData the installation data
     * @param parent      the parent panel
     */
    public GUIDirFieldView(DirField field, GUIInstallData installData, IzPanel parent)
    {
        super(field);
        init(new DirInputField(field, parent, installData, getValidatorContainers()));
    }

}
