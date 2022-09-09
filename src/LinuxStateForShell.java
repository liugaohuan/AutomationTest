import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 远程调用Linux shell 命令
 * @author wei.Li by 14-9-2.
 */
public class LinuxStateForShell {

    public static final String MACHINE_VENDOR = "sudo dmidecode -t baseboard | grep Manufacturer |awk '{print $NF}'";
    public static final String MACHINE_MODEL = "sudo dmidecode -t baseboard |grep Product |awk '{print $NF}'";
    public static final String OS_VERSION = "cat /etc/product-info";
    public static final String CPU_MODEL = "sudo dmidecode -t processor |grep Version |awk '{for(i=2;i<=NF;++i) printf $i \" \";printf \"\\n\"}'";
    public static final String MEMORY_MODEL = "sudo dmidecode -t memory |grep Manufacturer |head -n1 |awk '{print $NF}'";
    public static final String MEMORY_SIZE = "sudo free -mh |grep Mem |awk '{print $2}'";
    public static final String BIOS_VENDOR = "sudo dmidecode -t bios |grep Version |awk '{for(i=1;i<=NF;++i) printf $i \" \";printf \"\\n\"}'";
    public static final String NETWORK_MODEL = "sudo lspci |grep Ethernet |awk '{for(i=4;i<=NF;++i) printf $i \" \";printf \"\\n\"}'";
    public static final String DISPLAY_CARD_MODEL = "sudo lspci |grep VGA |awk '{for(i=5;i<=NF;++i) printf $i \" \";printf \"\\n\"}'";
    public static final String DISK_MODEL = "sudo fdisk -l |grep Disk\\ model |awk '{for(i=3;i<=NF;++i) printf $i \" \";printf \"\\n\"}'";

    public static final String[] COMMANDS = {MACHINE_VENDOR, MACHINE_MODEL,OS_VERSION, CPU_MODEL, MEMORY_MODEL,MEMORY_SIZE, BIOS_VENDOR, NETWORK_MODEL, DISPLAY_CARD_MODEL, DISK_MODEL};

    private static Session session;

    /**
     * 连接到指定的HOST
     *
     * @return isConnect
     * @throws JSchException JSchException
     */
    private static boolean connect(String user, String passwd, String host) {
        JSch jsch = new JSch();
        try {
            session = jsch.getSession(user, host, 22);
            session.setPassword(passwd);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
        } catch (JSchException e) {
            e.printStackTrace();
            System.out.println("connect error !");
            return false;
        }
        return true;
    }

    /**
     * 远程连接Linux 服务器 执行相关的命令
     *
     * @param commands 执行的脚本
     * @param user     远程连接的用户名
     * @param passwd   远程连接的密码
     * @param host     远程连接的主机IP
     * @return 最终命令返回信息
     */
    public static Map<String, String> runDistanceShell(String[] commands, String user, String passwd, String host) {
        if (!connect(user, passwd, host)) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        StringBuilder stringBuffer;

        BufferedReader reader = null;
        Channel channel = null;
        try {
            for (String command : commands) {
                stringBuffer = new StringBuilder();
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                channel.connect();
                InputStream in = channel.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                String buf;
                while ((buf = reader.readLine()) != null) {
                    stringBuffer.append(buf.trim()).append(" ");
                }
                //每个命令存储自己返回数据
                if(command.contains("sudo dmidecode -t baseboard | grep Manufacturer")){
                    map.put("设备厂商:", stringBuffer.toString());
                }
                if(command.contains("sudo dmidecode -t baseboard |grep Product")){
                    map.put("设备型号:", stringBuffer.toString());
                }
                if(command.contains("product-info")){
                    map.put("系统版本:", stringBuffer.toString());
                }
                if(command.contains("sudo dmidecode -t processor |grep Version")){
                    map.put("处理器型号:", stringBuffer.toString());
                }
                if(command.contains("sudo dmidecode -t memory |grep Manufacturer")){
                    map.put("内存型号:", stringBuffer.toString());
                }
                if(command.contains("sudo free -mh |grep Mem")){
                    map.put("内存大小:", stringBuffer.toString());
                }
                if(command.contains("sudo dmidecode -t bios |grep Version ")){
                    map.put("固件厂商:", stringBuffer.toString());
                }
                if(command.contains("sudo lspci |grep Ethernet")){
                    map.put("网卡型号:", stringBuffer.toString());
                }
                if(command.contains("sudo lspci |grep VGA ")){
                    map.put("显卡型号:", stringBuffer.toString());
                }
                if(command.contains("sudo fdisk -l |grep Disk\\ model")){
                    map.put("磁盘型号:", stringBuffer.toString());
                }
            }
        } catch (IOException | JSchException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (channel != null) {
                channel.disconnect();
            }
            session.disconnect();
        }
        return map;
    }

    public static void main(String[] args) {
        Map<String, String> result = runDistanceShell(COMMANDS, "root", "1", "10.2.17.129");
        System.out.println("IP地址：10.2.17.129的设备信息如下:");
        assert result != null;
        for(String key : result.keySet()){
           if(result.get(key).length() == 0){
               System.out.println(key+"unknown");
           }else{
               System.out.println(key+result.get(key));
           }
        }
    }
}
