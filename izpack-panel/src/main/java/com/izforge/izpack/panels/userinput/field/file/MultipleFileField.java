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

package com.izforge.izpack.panels.userinput.field.file;

import java.util.ArrayList;
import java.util.List;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.exception.IzPackException;


/**
 * Multiple file field.
 *
 * @author Tim Anderson
 */
public class MultipleFileField extends AbstractFileField
{

    /**
     * The number of visible rows.
     */
    private final int visibleRows;

    /**
     * The preferred width of the field.
     */
    private final int width;

    /**
     * The preferred height of the field.
     */
    private final int height;

    /**
     * Determines if multiple variables should be created to hold the selected files.
     */
    private final boolean multipleVariables;

    /**
     * Variables modified by this field.
     */
    private final List<String> variables = new ArrayList<String>();


    /**
     * Constructs a {@code MultipleFileField}.
     *
     * @param reader      the reader to get field information from
     * @param installData the installation data
     * @throws IzPackException if the field cannot be read
     */
    public MultipleFileField(MultipleFileFieldReader reader, InstallData installData)
    {
        super(reader, installData);
        visibleRows = reader.getVisibleRows();
        width = reader.getPreferredWidth();
        height = reader.getPreferredHeight();
        multipleVariables = reader.getCreateMultipleVariables();
        variables.add(getVariable());
    }

    /**
     * Sets the values. If multiple variables are being used, this creates a variable for each value,
     * using the naming convention: name, name_1, name_2....
     * If a single variable is being used, the values are concatenated together separated by ';'.
     *
     * @param values the file names
     */
    public void setValues(List<String> values)
    {
        if (multipleVariables)
        {
            InstallData installData = getInstallData();
            variables.clear();
            String variable = getVariable();
            variables.add(variable);
            int index = 0;
            for (String value : values)
            {
                String newVariable = variable;
                if (index > 0)
                {
                    newVariable += "_" + index;
                    variables.add(newVariable);
                }
                index++;
                installData.setVariable(newVariable, value);
            }
        }
        else
        {
            StringBuilder buffer = new StringBuilder();
            for (String value : values)
            {
                buffer.append(value);
                buffer.append(";");
            }
            setValue(buffer.toString());
        }
    }

    /**
     * Returns all variables that this field updates.
     *
     * @return all variables that this field updates
     */
    @Override
    public List<String> getVariables()
    {
        return variables;
    }

    /**
     * Returns the number of visible rows.
     *
     * @return the number of visible rows, or {@code -1} if none is specified
     */
    public int getVisibleRows()
    {
        return visibleRows;
    }

    /**
     * Returns the preferred width of the field.
     *
     * @return the preferred width, or {@code -1} if none is specified
     */
    public int getPreferredWidth()
    {
        return width;
    }

    /**
     * Returns the preferred width of the field.
     *
     * @return the preferred height, or {@code -1} if none is specified
     */
    public int getPreferredHeight()
    {
        return height;
    }

    /**
     * Determines if multiple variables should be created to hold the selected files.
     *
     * @return {@code true} if multiple variables should be created; {@code false} if a single variable should be used
     */
    public boolean getCreateMultipleVariables()
    {
        return multipleVariables;
    }
}
