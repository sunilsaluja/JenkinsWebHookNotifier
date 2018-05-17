package io.jenkins.plugins.webHookNotifier;

public class PluginConfig{

    private String WebHookType;
    private String WebHookURL;
    private String AuthKey;
    private String AuthToken;
    private boolean NotifyStart;
    private boolean NotifySuccess;
    private boolean NotifyAborted;
    private boolean NotifyNotBuilt;
    private boolean NotifyFailed;


    public PluginConfig(String WebHookType,String WebHookURL, String AuthKey, String AuthToken, boolean NotifyStart,boolean NotifySuccess, boolean NotifyAborted,boolean NotifyNotBuilt,boolean NotifyFailed){
        this.WebHookType = WebHookType;
        this.WebHookURL = WebHookURL;
        this.AuthKey = AuthKey;
        this.AuthToken = AuthToken;
        this.NotifyStart = NotifyStart;
        this.NotifySuccess = NotifySuccess;
        this.NotifyAborted = NotifyAborted;
        this.NotifyNotBuilt = NotifyNotBuilt;
        this.NotifyFailed = NotifyFailed;
    }


    public String getWebHookType() {
        return WebHookType;
    }

    public String getWebHookURL() {
        return WebHookURL;
    }

    public String getAuthKey() {
        return AuthKey;
    }

    public String getAuthToken() {
        return AuthToken;
    }

    public boolean getNotifyStart(){
        return NotifyStart;
    }
    public boolean getNotifySuccess(){
        return NotifySuccess;
    }
    public boolean getNotifyAborted(){
        return NotifyAborted;
    }
    public boolean getNotifyNotBuilt(){
        return NotifyNotBuilt;
    }
    public boolean getNotifyFailed(){
        return NotifyFailed;
    }

}