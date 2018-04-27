package com.jaca;

import java.util.HashMap;
import java.util.Map;

import static com.jaca.ReportAdapterListener.addAdditionalExecInfo;

/**
 * Created by nibu.baby
 */
public class AdditionalInfo {

    private static HashMap<String, String> infoMap;
    static {
        infoMap = new HashMap<>();
    }
//    public AdditionalInfo() {
//        infoMap = new HashMap<>();
//    }

    /**
     * Adding extra execution related info
     * @param name
     * @param value
     */
    public static void addExecutionInfo(String name, String value) {
        infoMap.put(name, value);
    }
    public static void addExecutionInfo(HashMap<String,String> suiteInfo)
    {
        if (!suiteInfo.isEmpty()) {
            ///need to add
            suiteInfo.forEach((k, v) -> {
                addAdditionalExecInfo(k, v);
            });
        }

//        addAdditionalExecInfo("sample","sample");
    }
    HashMap<String, String> getInfoMap() {
        return infoMap;
    }


}
