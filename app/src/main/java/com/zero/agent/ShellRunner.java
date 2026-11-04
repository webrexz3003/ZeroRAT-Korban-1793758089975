package com.zero.agent;
import java.io.*;
public class ShellRunner {
    public static String run(String cmd) {
        StringBuilder sb = new StringBuilder();
        try { Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())); BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line; while ((line = br.readLine()) != null) sb.append(line).append("\n"); while ((line = er.readLine()) != null) sb.append("ERR: ").append(line).append("\n"); p.waitFor();
        } catch (Exception e) { sb.append("ERR: ").append(e.getMessage()); }
        return sb.toString();
    }
}