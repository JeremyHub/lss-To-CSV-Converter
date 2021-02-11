import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import org.xml.sax.*;

import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        //asks for user input
        Scanner s = new Scanner(System.in);
        System.out.println("Please type the path to your .lss (will be renamed to .xml) or .xml file:");
        String filePath = s.nextLine();
        System.out.println("Please type the desired name of your .csv (do not include .csv)");
        String csvName = s.nextLine() + ".csv";

        //replace .lss with .xml
        File oldlss = new File(filePath);
        String newFilePath = filePath.substring(0,filePath.length()-4) + ".xml";
        File newXML = new File(newFilePath);
        oldlss.renameTo(newXML);
        try {
            //loads data
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document data = builder.parse(newFilePath);
            NodeList attemptList = data.getElementsByTagName("Attempt");
            NodeList segmentData = data.getElementsByTagName("Segment");

            // starts list of runs
            Properties currentRun = new Properties();
            Properties[] runList = {currentRun};

            // populates runlist with correct amount of runs with correct IDs and dateTimeStarted
            for (int i=0; i<attemptList.getLength(); i++) {
                // loads id data
                Node attempt = attemptList.item(i);
                Element attemptElement = (Element) attempt;
                String id = attemptElement.getAttribute("id");
                String dateTimeStarted = attemptElement.getAttribute("started");

                //updates runlist to be 1 longer, and adds the current run
                runList = Arrays.copyOf(runList, runList.length + 1);
                currentRun = new Properties();
                currentRun.setProperty("id", id);
                currentRun.setProperty("dateTimeStarted", dateTimeStarted);
                runList[i] = currentRun;
            }

            String[] segmentNames = new String[segmentData.getLength()];
            //loops through segments, putting the data in the run list
            for (int i=0; i<segmentData.getLength(); i++) {
                //loads data
                Node segment = segmentData.item(i);
                Element segmentElement = (Element) segment;
                NodeList segmentChildren = segmentElement.getChildNodes();

                //puts segment name in list
                Node n = segmentChildren.item(1);
                Element childElement = (Element) n;
                String segmentName = childElement.getTextContent();
                segmentNames[i] = segmentName;

                //loop through all runs of this segment
                Node segmentHisotry = segmentChildren.item(9);
                Element timesElement = (Element) segmentHisotry;
                NodeList segmentTimes = timesElement.getChildNodes();
                for (int j = 0; j < segmentTimes.getLength(); j++) {
                    Node segmentTimeNode = segmentTimes.item(j);
                    if (segmentTimeNode.getNodeType()==Node.ELEMENT_NODE){
                        //setting up variables to use later
                        String timeID = "";
                        String timeStr = "";

                        //getting id of time
                        Element timeElement = (Element) segmentTimeNode;
                        timeID = timeElement.getAttribute("id");

                        //getting time str
                        NodeList childTime = timeElement.getChildNodes();
                        if (childTime.getLength() > 0) {
                            Node timeStrNode = childTime.item(1);
                            if (timeStrNode.getNodeType()==Node.ELEMENT_NODE) {
                                Element timeStrElement = (Element) timeStrNode;
                                timeStr = timeStrElement.getTextContent();
                            }
                        }

                        //add time to run
                        int index = findIndexOfPropertyWithIDInList(runList, timeID);
                        runList[index].setProperty(segmentName, timeStrToSec(timeStr));
                    }
                }
            }
            
            writeToCSV(runList, csvName, segmentNames);
            s.close();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToCSV(Properties[] runsList, String filename, String[] segmentNames) {
        try {
            FileWriter fw = new FileWriter(filename,true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            // writes the columns to the top
            String columns = "id,dateTimeStarted";
            // add all segment names
            for (int i = 0; i < segmentNames.length; i++) {
                columns += "," + segmentNames[i];
            }
            pw.println(columns);

            // writes all the data
            for (int i=0; i < runsList.length - 1; i++) {
                String runData = runsList[i].getProperty("id") + "," + runsList[i].getProperty("dateTimeStarted");
                // add all the segment times
                for (int j = 0; j < segmentNames.length; j++) {
                    runData = runData + "," + runsList[i].getProperty(segmentNames[j], "-1");
                }
                pw.println(runData);
            }

            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String timeStrToSec(String timeStr) {
        if (timeStr == "") return "";
        int hour = Integer.parseInt(timeStr.substring(0, 2));
        int min = Integer.parseInt(timeStr.substring(3, 5));
        Double sec = Double.parseDouble(timeStr.substring(6));
        min += hour * 60.0;
        sec += min * 60.0;
        return Double.toString(sec);
    }

    private static int findIndexOfPropertyWithIDInList (Properties[] propertyList, String id) {
        for (int i = 0; i < propertyList.length; i++) {
            if (propertyList[i].getProperty("id").equals(id)) {
                return i;
            }
        }
        return -1;
    }
}