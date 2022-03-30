package be.kuleuven.spite.iotsearhandon.client;

import be.distrinet.spite.iotsear.core.model.context.ContextAttribute;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private IoTSEARClient ioTSEARHandsOn;
    public Fields fields = new Fields();

    private JPanel mainFrame;
    private JPanel loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JButton loginButton;
    private JPanel loginButtonPanel;
    private JPanel midPanel1;
    private JButton actionButton1;
    private JLabel actionLabel1;
    private JButton actionButton2;
    private JButton actionButton3;
    private JLabel actionLabel2;
    private JLabel actionLabel3;
    private JPanel midPanel2;
    private JPanel midPanel3;
    private JButton nfcButton;


    public Client() {
        this.ioTSEARHandsOn = new IoTSEARClient(this);
        loginButton.addActionListener((e)-> loginLogout());
        actionButton1.addActionListener((e)->ioTSEARHandsOn.getDoorAccess());
        actionButton2.addActionListener((e)->ioTSEARHandsOn.getFileAccess("files:documentation.pdf", "read"));
        actionButton3.addActionListener((e)->ioTSEARHandsOn.getFileAccess("files:documentation.pdf", "read-hardened"));
        nfcButton.addActionListener((e)->ioTSEARHandsOn.retrieveNfcContext());
    }

    public static void main(String... args) {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for(Handler h: handlers){
            h.setLevel(Level.WARNING);
        }
        JFrame frame = new JFrame("ClientWindow");
        Client window = new Client();
        frame.setContentPane(window.mainFrame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    public void loginLogout() {

        this.fields.setUsername(this.usernameField.getText());
        this.fields.setPassword(new String(this.passwordField.getPassword()));

        if(!loginStatus){
            try {
                ioTSEARHandsOn.login();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            setLoggedIn(false);
        }
    }
    private boolean loginStatus = false;
    public void setLoggedIn(boolean loggedIn){
        if(loggedIn){
            usernameLabel.setText("Logged in!");
            passwordField.setVisible(false);
            usernameField.setVisible(false);
            passwordLabel.setVisible(false);
            loginButton.setText("Logout");
        }else{
            usernameLabel.setText("User Name:");
            passwordField.setVisible(true);
            usernameField.setVisible(true);
            passwordLabel.setVisible(true);
            loginButton.setText("Login");
        }
        this.loginStatus = loggedIn;
    }

    public void nfcRetrieved(ContextAttribute attribute){
        if(attribute.getValue().isEmpty())
            setTempButtonColor(nfcButton,Color.RED,5000);
        else setTempButtonColor(nfcButton,Color.GREEN, 30000);
    }

    public void doorOpened(boolean response){
        if(!response)
            setTempButtonColor(actionButton1,Color.RED,5000);
        else {
            setTempButtonColor(actionButton1, Color.GREEN, 5000);
            nfcButton.setContentAreaFilled(false);
            nfcButton.setOpaque(false);
        }
    }
    public void fileRequested(boolean response, String action){
        JButton button = actionButton2;
        if(action.contains("hardened")) button = actionButton3;
        if(!response)
            setTempButtonColor(button,Color.RED,5000);
        else setTempButtonColor(button,Color.GREEN, 5000);
    }
    public void setTempButtonColor(final JButton button, Color color, int time){

        button.setBackground(color);
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        new Thread(()->{
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            button.setOpaque(false);
            button.setContentAreaFilled(false);
        }).start();
    }


}
