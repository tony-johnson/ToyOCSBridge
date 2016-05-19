package toyocsbridge;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import toyocsbridge.State.StateChangeListener;

/**
 *
 * @author tonyj
 */
public class ToyOCSGUI extends javax.swing.JFrame {

    private ToyOCSBridge ocs;
    private Map<String, JComboBox> statusMap = new HashMap<>();

    /**
     * Creates new form ToyOCSGUI
     *
     * @param ocs
     */
    ToyOCSGUI(ToyOCSBridge ocs) {
        this.ocs = ocs;
        CCS ccs = ocs.getCCS();
        initComponents();
        AggregateStatus aggregateStatus = ccs.getAggregateStatus();
        for (State state : aggregateStatus.getStates()) {
            String name = state.getEnumClass().getSimpleName();
            Box box = Box.createHorizontalBox();
            box.add(new JLabel(name));
            box.add(Box.createHorizontalStrut(10));
            JComboBox combo = new JComboBox(state.getEnumClass().getEnumConstants());
            combo.setEditable(false);
            combo.setSelectedItem(state.getState());
            setReadonly(combo);
            box.add(combo);
            box.add(Box.createHorizontalGlue());
            statusPanel.add(box);
            statusMap.put(name, combo);
        }
        ccs.addStateChangeListener(new StateChangeListener() {

            @Override
            public void stateChanged(State state, Enum oldState) {
                SwingUtilities.invokeLater(() -> {
                    JComboBox combo = statusMap.get(state.getEnumClass().getSimpleName());
                    combo.setSelectedItem(state.getState());
                });

            }
        });
        filterComboBox.setModel(new DefaultComboBoxModel(ocs.getFCS().getAvailableFilters().toArray()));
        Logger logger = Logger.getLogger("toyocsbridge");
        TextAreaHandler handler = new TextAreaHandler();
        handler.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord record) {

                return String.format("[%tc] %s\n", record.getMillis(), formatMessage(record));
            }

        });
        logger.addHandler(handler);
    }

    /**
     * Taken from http://stackoverflow.com/questions/23500183
     */
    private void setReadonly(JComboBox combo) {
        Component editorComponent = combo.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField) {
            ((JTextField) editorComponent).setEditable(false);
        }

        for (Component childComponent : combo.getComponents()) {
            if (childComponent instanceof AbstractButton) {
                childComponent.setEnabled(false);
                final MouseListener[] listeners = childComponent.getListeners(MouseListener.class);
                for (MouseListener listener : listeners) {
                    childComponent.removeMouseListener(listener);
                }
            }
        }

        final MouseListener[] mouseListeners = combo.getListeners(MouseListener.class);
        for (MouseListener listener : mouseListeners) {
            combo.removeMouseListener(listener);
        }

        final KeyListener[] keyListeners = combo.getListeners(KeyListener.class);
        for (KeyListener keyListener : keyListeners) {
            combo.removeKeyListener(keyListener);
        }

        combo.setFocusable(false);

        //box.getActionMap().clear(); //no effect
        //box.getInputMap().clear();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        statusPanel = new javax.swing.JPanel();
        commandPanel2 = new javax.swing.JPanel();
        setAvailableButton = new javax.swing.JButton();
        revokeAvailableButton = new javax.swing.JButton();
        simulateFault = new javax.swing.JButton();
        clearFaultButton = new javax.swing.JButton();
        javax.swing.JPanel commandPanel = new javax.swing.JPanel();
        enterControlButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        standbyButton = new javax.swing.JButton();
        enableButton = new javax.swing.JButton();
        disableButton = new javax.swing.JButton();
        startTextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        javax.swing.JPanel commandPanel1 = new javax.swing.JPanel();
        initImageButton = new javax.swing.JButton();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        deltaTSpinner = new javax.swing.JSpinner();
        takeImagesButton = new javax.swing.JButton();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        nImagesSpinner = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        exposureSpinner = new javax.swing.JSpinner();
        openShutterCheckbox = new javax.swing.JCheckBox();
        filterButton = new javax.swing.JButton();
        filterComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        statusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));
        statusPanel.setLayout(new javax.swing.BoxLayout(statusPanel, javax.swing.BoxLayout.PAGE_AXIS));

        commandPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("CCS Commands"));

        setAvailableButton.setText("setAvailable");
        setAvailableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setAvailableButtonActionPerformed(evt);
            }
        });

        revokeAvailableButton.setText("revokeAvailable");
        revokeAvailableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revokeAvailableButtonActionPerformed(evt);
            }
        });

        simulateFault.setText("simulateFault");
        simulateFault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simulateFaultActionPerformed(evt);
            }
        });

        clearFaultButton.setText("clearFault");
        clearFaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearFaultButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout commandPanel2Layout = new javax.swing.GroupLayout(commandPanel2);
        commandPanel2.setLayout(commandPanel2Layout);
        commandPanel2Layout.setHorizontalGroup(
            commandPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanel2Layout.createSequentialGroup()
                .addGroup(commandPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(setAvailableButton)
                    .addComponent(revokeAvailableButton)
                    .addComponent(simulateFault)
                    .addComponent(clearFaultButton))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        commandPanel2Layout.setVerticalGroup(
            commandPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanel2Layout.createSequentialGroup()
                .addComponent(setAvailableButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(revokeAvailableButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(simulateFault)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearFaultButton)
                .addContainerGap())
        );

        commandPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("OCS Lifecycle Commands"));

        enterControlButton.setText("enterControl");
        enterControlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterControlButtonActionPerformed(evt);
            }
        });

        exitButton.setText("exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        startButton.setText("start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        standbyButton.setText("standby");
        standbyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                standbyButtonActionPerformed(evt);
            }
        });

        enableButton.setText("enable");
        enableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableButtonActionPerformed(evt);
            }
        });

        disableButton.setText("disable");
        disableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableButtonActionPerformed(evt);
            }
        });

        startTextField.setColumns(20);
        startTextField.setText("Normal");

        javax.swing.GroupLayout commandPanelLayout = new javax.swing.GroupLayout(commandPanel);
        commandPanel.setLayout(commandPanelLayout);
        commandPanelLayout.setHorizontalGroup(
            commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanelLayout.createSequentialGroup()
                .addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(enterControlButton)
                    .addComponent(exitButton)
                    .addGroup(commandPanelLayout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(startTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(standbyButton)
                    .addComponent(enableButton)
                    .addComponent(disableButton))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        commandPanelLayout.setVerticalGroup(
            commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanelLayout.createSequentialGroup()
                .addComponent(enterControlButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exitButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(startTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(standbyButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableButton))
        );

        logTextArea.setColumns(80);
        logTextArea.setRows(20);
        jScrollPane2.setViewportView(logTextArea);

        commandPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("OCS Camera Commands"));

        initImageButton.setText("initImage");
        initImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initImageButtonActionPerformed(evt);
            }
        });

        jLabel4.setLabelFor(deltaTSpinner);
        jLabel4.setText("deltaT");

        deltaTSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), Float.valueOf(15.0f), Float.valueOf(0.1f)));

        takeImagesButton.setText("takeImages");
        takeImagesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                takeImagesButtonActionPerformed(evt);
            }
        });

        jLabel5.setLabelFor(nImagesSpinner);
        jLabel5.setText("nImages");

        nImagesSpinner.setModel(new javax.swing.SpinnerNumberModel(2, 0, 20, 1));

        jLabel6.setLabelFor(exposureSpinner);
        jLabel6.setText("exposure");

        exposureSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(15.0f), Float.valueOf(0.0f), Float.valueOf(30.0f), Float.valueOf(1.0f)));

        openShutterCheckbox.setSelected(true);
        openShutterCheckbox.setText("openShutter");
        openShutterCheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        filterButton.setText("setFilter");
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterButtonActionPerformed(evt);
            }
        });

        filterComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout commandPanel1Layout = new javax.swing.GroupLayout(commandPanel1);
        commandPanel1.setLayout(commandPanel1Layout);
        commandPanel1Layout.setHorizontalGroup(
            commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanel1Layout.createSequentialGroup()
                .addGroup(commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(commandPanel1Layout.createSequentialGroup()
                        .addComponent(initImageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deltaTSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(commandPanel1Layout.createSequentialGroup()
                        .addComponent(takeImagesButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nImagesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exposureSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openShutterCheckbox))
                    .addGroup(commandPanel1Layout.createSequentialGroup()
                        .addComponent(filterButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        commandPanel1Layout.setVerticalGroup(
            commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanel1Layout.createSequentialGroup()
                .addGroup(commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(initImageButton)
                    .addComponent(jLabel4)
                    .addComponent(deltaTSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(takeImagesButton)
                    .addComponent(jLabel5)
                    .addComponent(nImagesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(exposureSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(openShutterCheckbox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(commandPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterButton)
                    .addComponent(filterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 995, Short.MAX_VALUE)
                    .addComponent(commandPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(commandPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(commandPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commandPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commandPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commandPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void enterControlButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterControlButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.enterControl(0);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_enterControlButtonActionPerformed

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.exit(0);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_exitButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        String configuration = startTextField.getText();
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.start(0, configuration);
                return null;
            }
        };
        sw.execute();

    }//GEN-LAST:event_startButtonActionPerformed

    private void initImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initImageButtonActionPerformed
        float deltaT = ((Number) deltaTSpinner.getModel().getValue()).floatValue();
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.initImage(0, deltaT);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_initImageButtonActionPerformed

    private void takeImagesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_takeImagesButtonActionPerformed
        int nImages = ((Number) nImagesSpinner.getModel().getValue()).intValue();
        float exposure = ((Number) exposureSpinner.getModel().getValue()).floatValue();
        boolean openShutter = openShutterCheckbox.isSelected();
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.takeImages(0, exposure, nImages, openShutter);
                return null;
            }
        };
        sw.execute();    }//GEN-LAST:event_takeImagesButtonActionPerformed

    private void filterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterButtonActionPerformed
        String filter = filterComboBox.getSelectedItem().toString();
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.setFilter(0, filter);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_filterButtonActionPerformed

    private void standbyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_standbyButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.standby(0);
                return null;
            }
        };
        sw.execute();    }//GEN-LAST:event_standbyButtonActionPerformed

    private void enableButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.enable(0);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_enableButtonActionPerformed

    private void disableButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disableButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.disable(0);
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_disableButtonActionPerformed

    private void setAvailableButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setAvailableButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.setAvailable();
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_setAvailableButtonActionPerformed

    private void revokeAvailableButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_revokeAvailableButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.revokeAvailable();
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_revokeAvailableButtonActionPerformed

    private void simulateFaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simulateFaultActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.simulateFault();
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_simulateFaultActionPerformed

    private void clearFaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearFaultButtonActionPerformed
        SwingWorker sw = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                ocs.clearFault();
                return null;
            }
        };
        sw.execute();
    }//GEN-LAST:event_clearFaultButtonActionPerformed
 
    private class TextAreaHandler extends StreamHandler {

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
            logTextArea.append(getFormatter().format(record));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearFaultButton;
    private javax.swing.JPanel commandPanel2;
    private javax.swing.JSpinner deltaTSpinner;
    private javax.swing.JButton disableButton;
    private javax.swing.JButton enableButton;
    private javax.swing.JButton enterControlButton;
    private javax.swing.JButton exitButton;
    private javax.swing.JSpinner exposureSpinner;
    private javax.swing.JButton filterButton;
    private javax.swing.JComboBox filterComboBox;
    private javax.swing.JButton initImageButton;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JSpinner nImagesSpinner;
    private javax.swing.JCheckBox openShutterCheckbox;
    private javax.swing.JButton revokeAvailableButton;
    private javax.swing.JButton setAvailableButton;
    private javax.swing.JButton simulateFault;
    private javax.swing.JButton standbyButton;
    private javax.swing.JButton startButton;
    private javax.swing.JTextField startTextField;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JButton takeImagesButton;
    // End of variables declaration//GEN-END:variables
}
