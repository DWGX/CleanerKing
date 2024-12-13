package org.CleanerKing.example;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class JLineExample {
    public static void main(String[] args) {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .dumb(true) // 使用 Dumb Terminal 作为回退选项
                    .build();

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            String line;
            while ((line = reader.readLine("请输入命令 > ")) != null) {
                if ("exit".equalsIgnoreCase(line.trim())) {
                    System.out.println("退出程序。");
                    break;
                }
                System.out.println("你输入了: " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
