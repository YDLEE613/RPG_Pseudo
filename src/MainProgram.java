import javax.print.DocFlavor;
import java.io.*;
import java.util.*;

public class MainProgram {
    // alias+logical, physical, actual
    private static Map<String, Map<String, String>> logicalActualTableNames = new HashMap<>();

    // alias+logical, List of key fields with Column Name
    private static Map<String, List<String>> keyFieldsMap = new HashMap<>();

    private static Map<String, String> fieldColumnNameMap = new HashMap<>();
    private static Map<String, String> columnFieldNameMap = new HashMap<>();

    //<table name, <columnName, condition>>
    private static Map<String, List<Criteria>> tableColumnConditionMap = new HashMap<>();
    private static List<String> parmsList = new ArrayList<>();
    private static FileReader file = null;
    private static BufferedWriter bw;
    private static String outputFileName = "";

    public static void main(String[] args) throws IOException {
        String inputPath = HardCodedValues.getTxtInputFilePath();
        String outputPath = HardCodedValues.getTxtOutputFilePath();

        file = new FileReader(inputPath);
        FileWriter fw = new FileWriter(outputPath + HardCodedValues.getTXT_Pseudo() + outputFileName);
        processTables();

        bw = new BufferedWriter(fw);
        printParmsList();
        printTablesMap();
        bw.close();
    }

    private static void processTables() throws IOException {
        BufferedReader br = new BufferedReader(file);

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.contains(HardCodedValues.getTxtTables())) { // DISK
                String logicalTableName = getLogicalTableNames(line);
                String aliasedLogicalTableName = getAliasTableName(logicalTableName);
                List<String> keyFieldsNamesTemp = getKeyFields(logicalTableName);
                List<String> keyFieldsNames = new ArrayList<>();

                if (keyFieldsNamesTemp != null) {
                    keyFieldsNamesTemp.forEach(each ->{
                        each = each.concat(HardCodedValues.getStringSlash() + fieldColumnNameMap.get(each) + HardCodedValues.getStringUnderScore());
                        keyFieldsNames.add(each);
                    });

                    keyFieldsMap.put(aliasedLogicalTableName, keyFieldsNames);
                }

                // can find actual table names in next 2 lines
                line = br.readLine();
                if (!line.contains(HardCodedValues.getTextRtv()) && !line.contains(HardCodedValues.getTextRtv())) {

                    line = br.readLine();
                    if (line.contains(HardCodedValues.getTextRtv()) || line.contains(HardCodedValues.getTextRsq())) {
                        // get actual table name
                        outputFileName = getPhysicalTableName(logicalTableName);
                        logicalActualTableNames.put(aliasedLogicalTableName, Map.of(getPhysicalTableName(logicalTableName), getActualTableNames(line)));
                    }
                } else {
                    // get actual table name
                    logicalActualTableNames.put(aliasedLogicalTableName, Map.of(getPhysicalTableName(logicalTableName), getActualTableNames(line)));
                }

            } else if (line.contains(HardCodedValues.getTxtEntryParms())) {
                // find list of parameters
                boolean end = false;

                // intentionally read an extra line
                line = br.readLine();

                for (line = br.readLine(); line != null && !end; line = br.readLine()) {
                    if (line.contains(HardCodedValues.getTextParm())) {
//                        // extract parmameter
                        String parmName = getParms(line);
                        if (!parmName.split(HardCodedValues.getStringSlash())[0].trim().equalsIgnoreCase(HardCodedValues.getStringUnderScore())) {
                            parmsList.add(parmName);
                        }
                    } else if (line.contains(HardCodedValues.getEntryParmDiv())) {
                        // break out of loop
                        end = true;
                    }
                }
            }
        }
    }

    private static List<String> getKeyFields(String logicalTableName) throws IOException {
        List<String> keyFields = null;
        File[] tables = new File(HardCodedValues.getTxtTablesPath()).listFiles();

        for (int i = 0; i < tables.length; i++) {
            if (tables[i].getName().contains(logicalTableName)) {
                // read file
                keyFields = getKeyFieldNames(tables[i].getName());
            }
        }
        return keyFields;
    }

    private static List<String> getKeyFieldNames(String fileName) throws IOException {
        FileReader file = new FileReader(HardCodedValues.getTxtTablesPath() + fileName);
        List<String> keyFields = new ArrayList<>();
        List<Criteria> criteriaList = new ArrayList<>();

        BufferedReader br = new BufferedReader(file);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // store keyfields in list
            if (line.contains(HardCodedValues.getTxtKeyFields())) {

                // key fields end indicator
                for (String key = br.readLine(); key != null; key = br.readLine()) {
                    if (key.contains(HardCodedValues.getTxtKeyFieldsDiv1()) || key.contains(HardCodedValues.getTxtKeyFieldsDiv2())) {
                        break;
                    }
                    key = key.replaceFirst(HardCodedValues.getTextKeySep(), HardCodedValues.getString_AT());
                    key = key.split(HardCodedValues.getString_AT())[1].trim().split(HardCodedValues.getStringSpace())[0];
                    keyFields.add(key);
                }
            }
            // store fields in map
            else if (line.contains(HardCodedValues.getTextText()) && !line.contains(HardCodedValues.getString_AT())) { // get each column
                String fieldName = line.split(HardCodedValues.getTextText())[0].replaceFirst(HardCodedValues.getTextColSep(), HardCodedValues.getString_AT());
                fieldName = fieldName.split(HardCodedValues.getString_AT())[1].trim();

                String colName = line.split(HardCodedValues.getTextText())[1];
                colName = colName.substring(2, colName.length() - 8).trim();
                colName = colName.substring(0, colName.length() - 2);

                fieldColumnNameMap.put(fieldName, removeSpace(colName));
                columnFieldNameMap.put(colName, fieldName);
            }else if(removeSpace(line).contains("Field")){
                String column = "";
                String cond = "";

                if(removeSpace(line).contains("Field")){
                    line = line.split(HardCodedValues.getStringColon())[1].trim();
                    column = line.substring(0, line.length()-8).trim();
                }

                line = br.readLine();
                if(removeSpace(line).contains("Condition:")){
                    line = line.split(HardCodedValues.getStringColon())[1].trim();
                    cond = line.substring(0, line.length()-8).trim().replaceAll("[^a-zA-Z0-9]", "");
                }

                criteriaList.add(new Criteria(column, cond));
            }
        }
        tableColumnConditionMap.put(getAliasName(fileName), criteriaList);

        return keyFields;
    }
    private static String removeSpace(String text){
       return text.replaceAll("\\s" , "");
    }
    private static String getAliasTableName(String logicalTableName) throws IOException {
        File[] tables = new File(HardCodedValues.getTxtTablesPath()).listFiles();
        String aliasedLogicalName = "";

        for (int i = 0; i < tables.length; i++) {
            if (tables[i].getName().contains(logicalTableName)) {
                // read file
                aliasedLogicalName = getAliasName(tables[i].getName()) + HardCodedValues.getStringSlash() + logicalTableName;
                break;
            }
        }
        return aliasedLogicalName;
    }

    private static String getAliasName(String fileName) throws IOException {
        String aliasName = "";
        FileReader file = new FileReader(HardCodedValues.getTxtTablesPath() + fileName);

        BufferedReader br = new BufferedReader(file);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // first line with TEXT has alias name
            if (line.contains(HardCodedValues.getTextText())) {
                line = line.replaceFirst(HardCodedValues.getTextAliasSep(), HardCodedValues.getString_AT() + HardCodedValues.getString_AT());
                aliasName = line.split(HardCodedValues.getString_AT() + HardCodedValues.getString_AT())[1].trim().split(HardCodedValues.getStringSpace())[0];

                // once get the alias name, break
                break;
            }

        }

        return aliasName;
    }

    private static void printParmsList() throws IOException {
        bw.write(HardCodedValues.getEntryParmsHeader());
        bw.newLine();
        parmsList.forEach(each -> {
            try {
                bw.write(each);
                bw.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        bw.newLine();

    }

    private static String getParms(String line) {
        StringBuilder sb = new StringBuilder("");

        sb.append(line.split(HardCodedValues.getTextParm())[0].replaceFirst(HardCodedValues.getString_C(), HardCodedValues.getString_AT()).split(HardCodedValues.getString_AT())[1].trim() + HardCodedValues.getStringSlash());

        String[] tmp = line.split(HardCodedValues.getTextParm())[1].split(HardCodedValues.getStringSpace());
        int spaceCount = 0;
        boolean end = false;
        for (int i = 20; i < tmp.length & !end; i++) {
            if (!tmp[i].isBlank() && !tmp[i].isEmpty()) {
                if (!(spaceCount >= 2)) {
                    sb.append(tmp[i]);
                    spaceCount = 0;
                } else {
                    sb.append(HardCodedValues.getStringUnderScore());
                    end = true;
                }
            } else {
                spaceCount++;
            }
        }
        return sb.toString();
    }

    private static void printTablesMap() throws IOException {
        bw.write(HardCodedValues.getTablesHeader());
        bw.newLine();

        // k: alias + logical name, v: physical name, actual name
        logicalActualTableNames.forEach((k, v) -> {
            try {
                bw.write(k + HardCodedValues.getStringSlash());

                // a: physical name, b: actual name
                v.forEach((a, b) -> {
                    try {
                        bw.write(a + HardCodedValues.getStringSlash() + b + HardCodedValues.getStringUnderScore());
                        bw.newLine();

                        // k: alias + logical name, v: list of key fields
                        keyFieldsMap.forEach((c, d) -> {
                            if (c.equalsIgnoreCase(k)) {

                                // add KeysOrderBy text
                                try {
                                    bw.write(HardCodedValues.getStringTab() + HardCodedValues.getStringTab() + HardCodedValues.getTxtKeysOrderBy());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // append each key
                                d.forEach(each -> {
                                    try {
                                        bw.write(each + HardCodedValues.getStringTab());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        });
                        bw.newLine();

                        // add criteria
                        tableColumnConditionMap.forEach((table,criteriaList)->{
                            if(k.contains(table)){
                                criteriaList.forEach(each->{
                                    try {
                                        bw.write(HardCodedValues.getStringTab() + HardCodedValues.getStringTab()+ "  -  "
                                        + columnFieldNameMap.get(each.getField()) + HardCodedValues.getStringSlash() + removeSpace(each.getField()) + HardCodedValues.getStringUnderScore()
                                        + HardCodedValues.getStringSpace() + " = " + HardCodedValues.getStringSpace() + each.getCondition());
                                        bw.newLine();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String getPhysicalTableName(String logicalName) {
        return logicalName.substring(0, logicalName.length() - 2) + "P";
    }

    private static String getLogicalTableNames(String line) {
        return line.split(HardCodedValues.getStringSpace())[HardCodedValues.getIntTableIndex()].substring(1, line.split(HardCodedValues.getStringSpace())[HardCodedValues.getIntTableIndex()].length() - 2);
    }

    private static String getActualTableNames(String line) {
        String[] splittedLine = line.split(HardCodedValues.getStringColon())[1].trim().split(HardCodedValues.getStringSpace());

        int spaceCount = 0;
        String physicalName = "";
        for (int i = 0; i < splittedLine.length; i++) {

            if (splittedLine[i].isEmpty() || splittedLine[i].isBlank() || splittedLine[i].equalsIgnoreCase(HardCodedValues.getStringSpace())) {
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
