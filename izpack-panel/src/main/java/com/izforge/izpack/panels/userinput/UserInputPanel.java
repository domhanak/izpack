/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
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
package com.izforge.izpack.panels.userinput;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.data.binding.OsModel;
import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.gui.TwoColumnLayout;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;
import com.izforge.izpack.panels.userinput.field.ElementReader;
import com.izforge.izpack.panels.userinput.field.Field;
import com.izforge.izpack.panels.userinput.field.FieldFactory;
import com.izforge.izpack.panels.userinput.field.FieldHelper;
import com.izforge.izpack.panels.userinput.field.FieldView;
import com.izforge.izpack.panels.userinput.field.UserInputPanelSpec;
import com.izforge.izpack.panels.userinput.gui.Component;
import com.izforge.izpack.panels.userinput.gui.GUIFieldView;
import com.izforge.izpack.panels.userinput.gui.GUIFieldViewFactory;
import com.izforge.izpack.panels.userinput.gui.UpdateListener;
import com.izforge.izpack.util.OsConstraintHelper;
import com.izforge.izpack.util.PlatformModelMatcher;

/**
 * User input panel.
 *
 * @author Anthonin Bonnefoy
 */
public class UserInputPanel extends IzPanel
{
    private static final String FIELD_NODE_ID = "field";

    private static final String TOPBUFFER = "topBuffer";

    protected static final String ATTRIBUTE_CONDITIONID_NAME = "conditionid";

    protected static final String VARIABLE_NODE = "variable";

    protected static final String ATTRIBUTE_VARIABLE_NAME = "name";

    protected static final String ATTRIBUTE_VARIABLE_VALUE = "value";

    /**
     * The parsed result from reading the XML specification from the file
     */
    private IXMLElement spec;

    private boolean eventsActivated = false;

    private Set<String> variables = new HashSet<String>();
    private List<GUIFieldView> views = new ArrayList<GUIFieldView>();

    private JPanel panel;
    private RulesEngine rules;

    /**
     * The factory for creating validators.
     */
    private final ObjectFactory factory;

    /**
     * The platform-model matcher.
     */
    private final PlatformModelMatcher matcher;

    private UserInputPanelSpec userInputModel;

    /*--------------------------------------------------------------------------*/
    // This method can be used to search for layout problems. If this class is
    // compiled with this method uncommented, the layout guides will be shown
    // on the panel, making it possible to see if all components are placed
    // correctly.
    /*--------------------------------------------------------------------------*/
    // public void paint (Graphics graphics)
    // {
    // super.paint (graphics);
    // layout.showRules ((Graphics2D)graphics, Color.red);
    // }
    /*--------------------------------------------------------------------------*/

    /**
     * Constructs an <code>UserInputPanel</code>.
     *
     * @param panel       the panel meta-data
     * @param parent      the parent IzPack installer frame
     * @param installData the installation data
     * @param resources   the resources
     * @param rules       the rules engine
     * @param factory     factory
     * @param matcher     the platform-model matcher
     */
    public UserInputPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources,
                          RulesEngine rules, ObjectFactory factory, PlatformModelMatcher matcher)
    {
        super(panel, parent, installData, resources);

        this.rules = rules;
        this.factory = factory;
        this.matcher = matcher;
    }

    /**
     * Indicates wether the panel has been validated or not. The installer won't let the user go
     * further through the installation process until the panel is validated.
     *
     * @return a boolean stating whether the panel has been validated or not.
     */
    @Override
    public boolean isValidated()
    {
        return readInput();
    }

    /**
     * This method is called when the panel becomes active.
     */
    @Override
    public void panelActivate()
    {
        this.init();

        if (spec == null)
        {
            // TODO: translate
            emitError("User input specification could not be found.",
                      "The specification for the user input panel could not be found. Please contact the packager.");
            parent.skipPanel();
        }
        else
        {
            // update UI with current values of associated variables
            updateUIElements();

            ElementReader reader = new ElementReader(userInputModel.getConfig());
            List<String> forPacks = reader.getPacks(spec);
            List<String> forUnselectedPacks = reader.getUnselectedPacks(spec);
            List<OsModel> forOs = reader.getOsModels(spec);

            if (!FieldHelper.isRequiredForPacks(forPacks, installData.getSelectedPacks())
                    || !FieldHelper.isRequiredForUnselectedPacks(forUnselectedPacks, installData.getSelectedPacks())
                    || !matcher.matchesCurrentPlatform(forOs))
            {
                parent.skipPanel();
            }
            else
            {
                buildUI();

                Dimension size = getMaximumSize();
                setSize(size.width, size.height);
                validate();
                parent.lockPrevButton();
            }
        }
    }

    /**
     * Asks the panel to set its own XML installDataGUI that can be brought back for an automated installation
     * process. Use it as a blackbox if your panel needs to do something even in automated mode.
     *
     * @param panelRoot The XML root element of the panels blackbox tree.
     */
    @Override
    public void makeXMLData(IXMLElement panelRoot)
    {
        Map<String, String> entryMap = new HashMap<String, String>();

        for (String variable : variables)
        {
            entryMap.put(variable, installData.getVariable(variable));
        }
        for (FieldView view : views)
        {
            String variable = view.getField().getVariable();
            if (variable != null)
            {
                entryMap.put(variable, installData.getVariable(variable));
            }
        }

        new UserInputPanelAutomationHelper(entryMap).makeXMLData(installData, panelRoot);
    }

    private void init()
    {
        eventsActivated = false;
        TwoColumnLayout layout;
        super.removeAll();
        views.clear();

        // ----------------------------------------------------
        // read the specifications
        // ----------------------------------------------------
        if (spec == null)
        {
            spec = readSpec();
        }

        // ----------------------------------------------------
        // Set the topBuffer from the attribute. topBuffer=0 is useful
        // if you don't want your panel to be moved up and down during
        // dynamic validation (showing and hiding components within the
        // same panel)
        // ----------------------------------------------------
        int topbuff = 25;
        try
        {
            topbuff = Integer.parseInt(spec.getAttribute(TOPBUFFER));
        }
        catch (Exception ignore)
        {
            // do nothing
        }
        finally
        {
            layout = new TwoColumnLayout(10, 5, 30, topbuff, TwoColumnLayout.LEFT);
        }
        setLayout(new BorderLayout());

        panel = new JPanel();
        panel.setLayout(layout);

        if (spec == null)
        {
            // return if we could not read the spec. further
            // processing will only lead to problems. In this
            // case we must skip the panel when it gets activated.
            return;
        }

        // refresh variables specified in spec
        updateVariables();

        // ----------------------------------------------------
        // process all field nodes. Each field node is analyzed
        // for its type, then an appropriate member function
        // is called that will create the correct UI elements.
        // ----------------------------------------------------
        List<IXMLElement> fields = spec.getChildrenNamed(FIELD_NODE_ID);

        FieldFactory factory = new FieldFactory();
        GUIFieldViewFactory viewFactory = new GUIFieldViewFactory(installData, this, parent);
        UpdateListener listener = new UpdateListener()
        {
            @Override
            public void updated()
            {
                updateDialog();
            }
        };

        for (IXMLElement element : fields)
        {
            Field field = factory.create(element, userInputModel.getConfig(), installData, matcher);
            if (field.isConditionTrue())
            {
                GUIFieldView view = viewFactory.create(field);
                view.setUpdateListener(listener);
                views.add(view);
            }
        }
        eventsActivated = true;
    }

    protected void updateUIElements()
    {
        boolean updated = false;

        for (GUIFieldView view : views)
        {
            updated |= view.updateView();
        }
        if (updated)
        {
            super.invalidate();
        }
    }

    /**
     * Builds the UI and makes it ready for display
     */
    private void buildUI()
    {
        for (GUIFieldView view : views)
        {
            Field field = view.getField();
            if (FieldHelper.isRequired(field, installData, matcher))
            {
                if (!view.isDisplayed())
                {
                    view.setDisplayed(true);
                    for (Component components : view.getComponents())
                    {
                        panel.add(components.getComponent(), components.getConstraints());
                    }
                }
            }
            else
            {
                if (view.isDisplayed())
                {
                    view.setDisplayed(false);
                    for (Component element : view.getComponents())
                    {
                        panel.remove(element.getComponent());
                    }
                }
            }
        }

        JScrollPane scroller = new JScrollPane(panel);
        Border emptyBorder = BorderFactory.createEmptyBorder();
        scroller.setBorder(emptyBorder);
        scroller.setViewportBorder(emptyBorder);
        scroller.getVerticalScrollBar().setBorder(emptyBorder);
        scroller.getHorizontalScrollBar().setBorder(emptyBorder);
        add(scroller, BorderLayout.CENTER);
    }

    /**
     * Reads the input installDataGUI from all UI elements and sets the associated variables.
     *
     * @return <code>true</code> if the operation is successful, otherwise <code>false</code>.
     */
    private boolean readInput()
    {
        for (GUIFieldView view : views)
        {
            if (view.isDisplayed())
            {
                if (!view.updateField())
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Reads the XML specification for the panel layout.
     *
     * @return the panel specification
     * @throws IzPackException for any problems in reading the specification
     */
    private IXMLElement readSpec()
    {
        userInputModel = new UserInputPanelSpec(getResources(), installData, factory);
        return userInputModel.getPanelSpec(getMetadata());
    }

    protected void updateVariables()
    {
        /**
         * Look if there are new variables defined
         */
        List<IXMLElement> variables = spec.getChildrenNamed(VARIABLE_NODE);

        for (IXMLElement variable : variables)
        {
            String vname = variable.getAttribute(ATTRIBUTE_VARIABLE_NAME);
            String vvalue = variable.getAttribute(ATTRIBUTE_VARIABLE_VALUE);

            if (vvalue == null)
            {
                // try to read value element
                if (variable.hasChildren())
                {
                    IXMLElement value = variable.getFirstChildNamed("value");
                    vvalue = value.getContent();
                }
            }

            String conditionid = variable.getAttribute(ATTRIBUTE_CONDITIONID_NAME);
            if (conditionid != null)
            {
                // check if condition for this variable is fulfilled
                if (!rules.isConditionTrue(conditionid, this.installData))
                {
                    continue;
                }
            }
            // are there any OS-Constraints?
            List<OsModel> osList = OsConstraintHelper.getOsList(variable);
            if (matcher.matchesCurrentPlatform(osList))
            {
                if (vname != null)
                {
                    if (vvalue != null)
                    {
                        // substitute variables in value field
                        vvalue = replaceVariables(vvalue);

                        // try to cut out circular references
                        installData.setVariable(vname, "");
                        vvalue = replaceVariables(vvalue);
                    }
                    // set variable
                    installData.setVariable(vname, vvalue);

                    // for save this variable to be used later by Automation Helper
                    this.variables.add(vname);
                }
            }
        }
    }

    private void updateDialog()
    {
        if (this.eventsActivated)
        {
            this.eventsActivated = false;
            if (isValidated())
            {
                // read input
                // and update elements
                init();
                updateVariables();
                updateUIElements();
                buildUI();
                validate();
                repaint();
            }
            this.eventsActivated = true;
        }
    }

    /**
     * Helper to replace variables in text.
     *
     * @param text the text to perform replacement on. May be {@code null}
     * @return the text with any variables replaced with their values
     */
    private String replaceVariables(String text)
    {
        return installData.getVariables().replace(text);
    }

}
