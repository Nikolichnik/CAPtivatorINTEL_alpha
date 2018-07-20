package testCAPtivatorINTEL_alpha;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.SerialPort;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class CAPtivatorINTEL_alpha {

    static SerialPort chosenPort;
    static PrintWriter writer = null;
    static boolean writeToFileBool;
    static int x = 0;
    static double voltage = 0;
    static double current = 0;
    static double seconds = 0;

    public static void main(String[] args) {

        // create and configure the window
        JFrame window = new JFrame();
        window.setTitle("CAPtivatorINTEL_alpha");
        window.setSize(600, 400);
        ImageIcon icon = new ImageIcon("resources/icon.png");
        window.setIconImage(icon.getImage());
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayout(1, 7, 3, 0));

        // create a drop-down box and connect button
        JComboBox<String> portList = new JComboBox<String>();
        JButton connectButton = new JButton("Connect");

        // create checkbox triggering writing to a file
        JTextField fileDestination = new JTextField("Enter destination file name here");
        fileDestination.setPreferredSize(new Dimension(130, 14));
        JCheckBox writeToFile = new JCheckBox("Write to file");

        // create file selection and button for triggering readout from file
        JTextField fileToRead = new JTextField("sample1");
        fileToRead.setPreferredSize(new Dimension(130, 14));
        JButton fileReadButton = new JButton("Read from file");

        JPanel tiny = new JPanel();
        tiny.setLayout(new GridLayout(1, 4, 0, 0));
//        tiny.add(Box.createRigidArea(new Dimension(110,0)));        
        tiny.add(writeToFile);

        JPanel containerLeft = new JPanel();
        containerLeft.setLayout(new GridLayout(2, 1, 1, 1));
        containerLeft.add(fileToRead);
        containerLeft.add(fileReadButton);

        JPanel containerMiddle = new JPanel();
        containerMiddle.setLayout(new GridLayout(2, 1, 1, 1));
        containerMiddle.add(fileDestination);
        containerMiddle.add(tiny);

        JPanel containerRight = new JPanel();
        containerRight.setLayout(new GridLayout(2, 1, 1, 1));
        containerRight.add(portList);
        containerRight.add(connectButton);

        JPanel spacer = new JPanel();

        bottomPanel.add(containerLeft);
        bottomPanel.add(spacer);
        bottomPanel.add(spacer);
        bottomPanel.add(spacer);
        bottomPanel.add(spacer);
        bottomPanel.add(containerMiddle);
        bottomPanel.add(containerRight);

        window.add(bottomPanel, BorderLayout.SOUTH);

        // populate the drop-down box
        SerialPort[] portNames = SerialPort.getCommPorts();
        for (int i = 0; i < portNames.length; i++) {
            portList.addItem(portNames[i].getSystemPortName());
        }

        // create the line graph
        XYSeries seriesVoltage = new XYSeries("Voltage");
        XYSeries seriesCurrent = new XYSeries("Current");
        XYSeriesCollection datasetVoltage = new XYSeriesCollection(seriesVoltage);
        XYSeriesCollection datasetCurrent = new XYSeriesCollection(seriesCurrent);
        JFreeChart chartVoltage = ChartFactory.createXYLineChart("Voltage", "time [s]", "U [mV]", datasetVoltage);
        JFreeChart chartCurrent = ChartFactory.createXYLineChart("Current", "time [s]", "I [mA]", datasetCurrent);
        window.add(new ChartPanel(chartVoltage), BorderLayout.EAST);
        window.add(new ChartPanel(chartCurrent), BorderLayout.WEST);

        // configure the connect button and use another thread to listen for data
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (connectButton.getText().equals("Connect")) {
                    // attempt to connect to the serial port
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if (chosenPort.openPort()) {
                        connectButton.setText("Disconnect");
                        portList.setEnabled(false);
                    }
                    try {
                        if (writeToFile.isSelected() && fileDestination.getText() != null && fileDestination.getText() != "Enter destination file name here") {
                            FileWriter fw = new FileWriter("data/" + fileDestination.getText() + ".txt", true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            writer = new PrintWriter(bw);
                        }
                    } catch (IOException ex) {
                        System.out.println("Something's wrong with file!");
                    }

                    // create a new thread that listens for incoming text and populates the graph
                    Thread thread = new Thread() {
                        @Override
                        public void run() {

                            Scanner scanner = new Scanner(chosenPort.getInputStream());

                            while (scanner.hasNextLine()) {

                                try {
                                    String line = scanner.nextLine();
                                    System.out.println(line);
                                    List<Double> linijaPodataka = new ArrayList();
                                    if (line.matches("\\d+,.*")) {
                                        linijaPodataka = Collections.list(new StringTokenizer(line, ",", false)).stream().map(token -> Double.parseDouble((String) token)).collect(Collectors.toList());
                                        voltage = linijaPodataka.get(0);
                                        current = linijaPodataka.get(1);
                                        seconds = linijaPodataka.get(2);
                                    }
                                    seriesVoltage.add(seconds, voltage);
                                    seriesCurrent.add(seconds, current);
                                    linijaPodataka.clear();
                                    window.repaint();
                                } catch (Exception ex) {
                                    System.out.println("Something is wrong with parsing!");
                                }
                                if (writer != null) {
                                    writer.append(voltage + ", " + current + ", " + seconds + "\r\n");
                                    writer.flush();
                                }
                            }
                            scanner.close();
                        }
                    };
                    thread.start();
                } else {
                    // disconnect from the serial port
                    chosenPort.closePort();
                    portList.setEnabled(true);
                    connectButton.setText("Connect");
                    seriesVoltage.clear();
                    seriesCurrent.clear();
                    x = 0;
                }
            }
        }
        );

        fileReadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    File file = new File("data/" + fileToRead.getText() + ".txt");
                    Scanner sc = new Scanner(file);
                    if (fileReadButton.getText().equals("Read from file")) {
                        fileReadButton.setText("Reading from file - click to reset");

                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                List<Double> linijaPodataka = new ArrayList();
                                String line = "";
                                while (sc.hasNextLine()) {
                                    line = sc.nextLine();
                                    linijaPodataka = Collections.list(new StringTokenizer(line, ",", false)).stream().map(token -> Double.parseDouble((String) token)).collect(Collectors.toList());
                                    voltage = linijaPodataka.get(0);
                                    current = linijaPodataka.get(1);
                                    seconds = linijaPodataka.get(2);
                                    seriesVoltage.add(seconds, voltage);
                                    seriesCurrent.add(seconds, current);
                                    linijaPodataka.clear();
                                    window.repaint();
                                }
                            }
                        };
                        thread.start();
                    } else {
                        fileReadButton.setText("Read from file");
                        seriesVoltage.clear();
                        seriesCurrent.clear();
                        x = 0;
                    }
                } catch (FileNotFoundException ex) {
                    System.out.println("Unable to open file '" + fileToRead.getText() + ".txt'!");
                    fileReadButton.setText("Read from file");
                    JOptionPane.showMessageDialog(null, "File  '" + fileToRead.getText() + ".txt' not found!");
                }
            }
        }
        );
        // show the window
        window.setVisible(true);
    }
}
