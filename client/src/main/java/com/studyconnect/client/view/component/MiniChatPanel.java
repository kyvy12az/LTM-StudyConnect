package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Local-only chat UI. Replace appendOutgoingMessage with a controller call when TCP chat is available. */
public class MiniChatPanel extends MainTheme.RoundedPanel {
    private final JPanel messages=new JPanel();
    private final JTextField input=new JTextField();
    private final JPanel body=new JPanel(new BorderLayout());
    private boolean minimized;

    public MiniChatPanel(){
        super(18);setLayout(new BorderLayout());setPreferredSize(new Dimension(330,300));setMaximumSize(new Dimension(Integer.MAX_VALUE,300));
        add(createHeader(),BorderLayout.NORTH);
        body.setOpaque(false);body.add(createMessages(),BorderLayout.CENTER);body.add(createFooter(),BorderLayout.SOUTH);add(body,BorderLayout.CENTER);
        addIncoming("Chiều nay bạn rảnh không? Mình xem lại bài Java Swing nhé!");
        addOutgoing("Được nè! Mình cũng đang làm bài tập.");
    }
    private JComponent createHeader(){
        JPanel header=new JPanel(new BorderLayout(8,0));header.setBackground(MainTheme.TEAL_DARK);header.setBorder(new EmptyBorder(8,10,8,8));
        AvatarView avatar=new AvatarView("Nguyễn Linh Chi",36);avatar.setOnline(true);
        JPanel identity=new JPanel();identity.setOpaque(false);identity.setLayout(new BoxLayout(identity,BoxLayout.Y_AXIS));
        JLabel name=new JLabel("Nguyễn Linh Chi");name.setFont(MainTheme.font(Font.BOLD,12));name.setForeground(Color.WHITE);
        JLabel state=new JLabel("●  Đang trực tuyến");state.setFont(MainTheme.font(Font.PLAIN,10));state.setForeground(new Color(190,255,222));identity.add(name);identity.add(state);
        JPanel person=new JPanel(new BorderLayout(7,0));person.setOpaque(false);person.add(avatar,BorderLayout.WEST);person.add(identity,BorderLayout.CENTER);header.add(person,BorderLayout.CENTER);
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));tools.setOpaque(false);
        tools.add(tool(MainTheme.IconType.VIDEO,"Gọi video",null));tools.add(tool(MainTheme.IconType.PHONE,"Gọi thoại",null));
        JButton min=tool(MainTheme.IconType.MORE,"Thu nhỏ",null);min.setText("−");min.setForeground(Color.WHITE);min.setIcon(null);min.addActionListener(e->toggle());tools.add(min);
        JButton close=tool(MainTheme.IconType.MORE,"Đóng",null);close.setText("×");close.setForeground(Color.WHITE);close.setIcon(null);close.addActionListener(e->setVisible(false));tools.add(close);header.add(tools,BorderLayout.EAST);return header;
    }
    private JButton tool(MainTheme.IconType type,String tip,java.awt.event.ActionListener listener){
        JButton b=new JButton(MainTheme.svgIcon(type,17,Color.WHITE));b.setToolTipText(tip);b.setBorder(new EmptyBorder(5,6,5,6));b.setContentAreaFilled(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));if(listener!=null)b.addActionListener(listener);return b;
    }
    private JComponent createMessages(){
        messages.setOpaque(false);messages.setLayout(new BoxLayout(messages,BoxLayout.Y_AXIS));messages.setBorder(new EmptyBorder(8,8,8,8));JScrollPane scroll=MainTheme.scrollPane(messages);scroll.setPreferredSize(new Dimension(310,170));return scroll;
    }
    private JComponent createFooter() {
        JPanel footer = new JPanel(new BorderLayout(6, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(7, 8, 8, 8));

        // Nút cảm xúc
        JButton smileButton = MainTheme.iconButton(
                MainTheme.IconType.SMILE,
                16,
                new Color(245, 166, 35),
                "Cảm xúc"
        );
        smileButton.setPreferredSize(new Dimension(36, 36));

        footer.add(smileButton, BorderLayout.WEST);

        // Ô nhập tin nhắn
        input.setFont(MainTheme.font(Font.PLAIN, 12));
        input.setToolTipText("Nhập tin nhắn...");
        input.setPreferredSize(new Dimension(100, 36));

        input.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                MainTheme.BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(0, 10, 0, 10)
                )
        );

        input.addActionListener(event -> send());

        footer.add(input, BorderLayout.CENTER);

        // Khu vực nút bên phải
        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 2, 0)
        );
        actionPanel.setOpaque(false);

        JButton attachButton = MainTheme.iconButton(
                MainTheme.IconType.ATTACH,
                16,
                MainTheme.MUTED,
                "Đính kèm"
        );
        attachButton.setPreferredSize(new Dimension(36, 36));

        JButton sendButton = MainTheme.iconButton(
                MainTheme.IconType.SEND,
                16,
                MainTheme.TEAL,
                "Gửi"
        );
        sendButton.setPreferredSize(new Dimension(36, 36));
        sendButton.addActionListener(event -> send());

        actionPanel.add(attachButton);
        actionPanel.add(sendButton);

        footer.add(actionPanel, BorderLayout.EAST);

        return footer;
    }
    private void send(){String text=input.getText().trim();if(text.isEmpty())return;addOutgoing(text);input.setText("");}
    private void addIncoming(String text){addBubble(text,false);}
    private void addOutgoing(String text){addBubble(text,true);}
    private void addBubble(String text,boolean mine){
        JPanel row=new JPanel(new FlowLayout(mine?FlowLayout.RIGHT:FlowLayout.LEFT,0,3));row.setOpaque(false);JLabel bubble=new JLabel("<html><div style='width:220px'>"+escape(text)+"</div></html>");bubble.setFont(MainTheme.font(Font.PLAIN,11));bubble.setForeground(MainTheme.TEXT);bubble.setOpaque(true);bubble.setBackground(mine?new Color(214,248,230):new Color(238,243,246));bubble.setBorder(new EmptyBorder(7,9,7,9));row.add(bubble);row.setAlignmentX(Component.LEFT_ALIGNMENT);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,bubble.getPreferredSize().height+8));messages.add(row);messages.revalidate();messages.repaint();
    }
    private void toggle(){minimized=!minimized;body.setVisible(!minimized);setPreferredSize(new Dimension(330,minimized?58:300));setMaximumSize(new Dimension(Integer.MAX_VALUE,minimized?58:300));revalidate();}
    private String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
