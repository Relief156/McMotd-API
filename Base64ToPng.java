import com.alibaba.fastjson.JSONObject;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;

public class Base64ToPng {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("请提供服务器地址作为参数，例如：java Base64ToPng mcsy.net");
            return;
        }
        
        String serverAddress = args[0];
        String outputFileName = serverAddress + ".png";
        
        try {
            // 从/raw端点获取JSON数据
            URL url = new URL("http://localhost:8082/raw/" + serverAddress);
            StringBuilder jsonResponse = new StringBuilder();
            
            try (InputStream inputStream = url.openStream();
                 Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                while (scanner.hasNextLine()) {
                    jsonResponse.append(scanner.nextLine());
                }
            }
            
            // 解析JSON数据
            JSONObject jsonObject = JSONObject.parseObject(jsonResponse.toString());
            
            // 提取base64编码的图像数据
            String base64Image = jsonObject.getString("favicon");
            
            // 去除可能的data URL前缀
            String imageData = base64Image;
            if (base64Image.startsWith("data:image/png;base64,")) {
                imageData = base64Image.substring("data:image/png;base64,".length());
            }
            
            // 解码base64数据
            byte[] imageBytes = Base64.getDecoder().decode(imageData);
            
            // 保存为PNG文件
            try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                fos.write(imageBytes);
            }
            
            System.out.println("成功将base64图像转换为PNG文件：" + outputFileName);
            System.out.println("文件大小：" + imageBytes.length + " 字节");
        } catch (Exception e) {
            System.out.println("转换失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}