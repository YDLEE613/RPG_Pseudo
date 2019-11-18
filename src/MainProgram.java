import javax.print.DocFlavor;
import java.io.*;
import java.util.*;

public class MainProgram {
    private static  String TXT_INPUT_FILE_PATH;
    private static  String TXT_OUTPUT_FILE_PATH;
    private static  String TXT_OUTPUT_FILE_NAME;
    private static final String TXT_TABLES = "DISK";
    private static final String TXT_ENTRY_PARMS = "*ENTRY    PLIST";
    private static final String STRING_SPACE = " ";
    private static final String STRING_COMMA = ",";
    private static final String STRING_SLASH = "/";
    private static final String STRING_COLON = ":";
    private static final String STRING_UNDER_SCORE = "_";
    private static final int INT_TABLE_INDEX = 7;

    private static final String TABLES_HEADER = "*** Tables ***";
    private static final String ENTRY_PARMS_HEADER = "*** Entry Parms ***";
    private static final String TEXT_RTV = "RTV";
    private static final String TEXT_RSQ = "RSQ";
    private static final String TEXT_PARM = "PARM";

    private static final String END_OF_TABLES = "Long constants";
    private static final String ENTRY_PARM_DIV = "*****************************************************************";


    private static Map<String, Map<String, String>> logicalActualTableNames = new HashMap<>();
    private static List<String> parmsList = new ArrayList<>();
    private static FileReader file = null;
    private static BufferedWriter bw;

    public static void main(String[] args) throws IOException {
        TXT_INPUT_FILE_PATH = args[0];
        file = new FileReader(TXT_INPUT_FILE_PATH);

        TXT_OUTPUT_FILE_PATH = args[1];

        TXT_OUTPUT_FILE_NAME = args[2];
        bw = new BufferedWriter(new FileWriter(TXT_OUTPUT_FILE_PATH + TXT_OUTPUT_FILE_NAME));

        processTables();

        printParmsList();
        bw.newLine();
        printTablesMap();

        bw.close();
    }

    private static void processTables() throws IOException {
        BufferedReader br = new BufferedReader(file);

        int count = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.contains(TXT_TABLES)) { // DISK
                String logicalTableName = getLogicalTableNames(line);

                // can find actual table names in next 2 lines
                line = br.readLine();
                if (!line.contains(TEXT_RTV) && !line.contains(TEXT_RSQ)) {

                    line = br.readLine();
                    if (line.contains(TEXT_RTV) || line.contains(TEXT_RSQ)) {
                        // get actual table name
                        logicalActualTableNames.put(logicalTableName, Map.of(getPhysicalTableName(logicalTableName), getActualTableNames(line)));
                    }
                } else {
                    // get actual table name
                    logicalActualTableNames.put(logicalTableName, Map.of(getPhysicalTableName(logicalTableName), getActualTableNames(line)));
                }

            } else if (line.contains(TXT_ENTRY_PARMS)) {
                // find list of parameters
                boolean end = false;

                for (line = br.readLine(); line != null && !end; line = br.readLine()) {
                    if (line.contains(TEXT_PARM)) {
                        // extract parmameter
                        String parmName = getParms(line);
                        if (!parmName.split(STRING_SLASH)[1].trim().equalsIgnoreCase(STRING_UNDER_SCORE)) {
                            parmsList.add(parmName);
                        }
                    } else if (line.contains(ENTRY_PARM_DIV)) {
                        // break out of loop
                        end = true;
                    }
                }
            }
        }
    }

    private static void printParmsList() throws IOException {
//        System.out.println(ENTRY_PARMS_HEADER);
        bw.write(ENTRY_PARMS_HEADER);
        bw.newLine();
        parmsList.forEach(each -> {
            try {
                bw.write(each);
                bw.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String getParms(String line) {
        StringBuilder sb = new StringBuilder("");

        sb.append(line.split(TEXT_PARM)[0].split("C")[1].trim() + STRING_SLASH);

        String[] tmp = line.split(TEXT_PARM)[1].split(" ");
        int spaceCount = 0;
        boolean end = false;
        for (int i = 20; i < tmp.length & !end; i++) {
            if (!tmp[i].isBlank() && !tmp[i].isEmpty()) {
                if (!(spaceCount >= 2)) {
                    sb.append(tmp[i]);
                    spaceCount = 0;
                } else {
                    sb.append(STRING_UNDER_SCORE);
                    end = true;
                }
            } else {
                spaceCount++;
            }
        }
        return sb.toString();
    }

    private static void printTablesMap() throws IOException {
        bw.write(TABLES_HEADER);
        bw.newLine();


//        System.out.println(TABLES_HEADER);
        logicalActualTableNames.forEach((k, v) -> {
            System.out.print(k + STRING_SLASH);
            v.forEach((a, b) -> {
                try {
                    bw.write(a + STRING_SLASH + b + STRING_UNDER_SCORE);
                    bw.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                System.out.println(a + STRING_SLASH + b + STRING_UNDER_SCORE);
            });
        });
    }

    private static String getPhysicalTableName(String logicalName) {
        return logicalName.substring(0, logicalName.length() - 2) + "P";
    }

    private static String getLogicalTableNames(String line) {
        return line.split(STRING_SPACE)[INT_TABLE_INDEX].substring(1, line.split(STRING_SPACE)[INT_TABLE_INDEX].length() - 2);
    }

    private static String getActualTableNames(String line) {
        String[] splittedLine = line.split(STRING_COLON)[1].trim().split(STRING_SPACE);

        int spaceCount = 0;
        String physicalName = "";
        for (int i = 0; i < splittedLine.length; i++) {

            if (splittedLine[i].isEmpty() || splittedLine[i].isBlank() || splittedLine[i].equalsIgnoreCase(STRING_SPACE)) {
                spaceCount++;

                if (spaceCount >= 2) {
                    return physicalName;
                }
                continue;
            } else {
                physicalName += splittedLine[i];
                spaceCount = 0;
            }
        }

        return null;
    }

}
