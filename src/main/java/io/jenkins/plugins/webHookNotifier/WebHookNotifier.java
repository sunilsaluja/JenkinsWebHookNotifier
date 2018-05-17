package io.jenkins.plugins.webHookNotifier;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Environment;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.triggers.SCMTrigger;
import hudson.util.ListBoxModel;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class WebHookNotifier extends Notifier implements SimpleBuildStep {

    private final String WebHookType;
    private final String WebHookURL;
    private final String AuthKey;
    private final String AuthToken;
    private final boolean NotifyStart;
    private final boolean NotifySuccess;
    private final boolean NotifyAborted;
    private final boolean NotifyNotBuilt;
    private final boolean NotifyFailed;

    private final String CommitSha1;

    //private JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();

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


    public String getCommitSha1() {
        return CommitSha1;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @DataBoundConstructor
    public WebHookNotifier(String webHookType, String webHookURL, String authKey, String authToken, String commitSha1,boolean notifyStart,boolean notifySuccess, boolean notifyAborted ,boolean notifyNotBuilt, boolean notifyFailed) {
        WebHookType = webHookType;
        WebHookURL = webHookURL;
        AuthKey = authKey;
        AuthToken = authToken;
        NotifyStart = notifyStart;
        NotifySuccess = notifySuccess;
        NotifyAborted = notifyAborted;
        NotifyNotBuilt = notifyNotBuilt;
        NotifyFailed = notifyFailed;
        CommitSha1 = commitSha1;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            try {
				Helper.getInstance().NotifyWebHook((Run<?, ?>)build, BuildState.STARTED, getConfig());
			} catch (Exception e) {
				listener.getLogger().println("Error while Notify Start:"+e.getMessage());
			}
            return true;
        // return processJenkinsEvent(build, null, listener, BuildState.INPROGRESS);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        return Helper.getInstance().perform(build, null, listener, false, getConfig());
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
            @Nonnull TaskListener listener) throws InterruptedException, IOException {
        //listener.getLogger().println("Hello, i am in perform!");
        if (!Helper.getInstance().perform(run, workspace, listener, false, getConfig())) {
            run.setResult(Result.FAILURE);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl) super.getDescriptor();
    }

    @Symbol({ "notifyWebHook" })
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String WebHookType;
        private String WebHookURL;
        private String AuthKey;
        private String AuthToken;
        private boolean NotifyStart;
        private boolean NotifySuccess;
        private boolean NotifyAborted;
        private boolean NotifyNotBuilt;
        private boolean NotifyFailed;

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

        public DescriptorImpl() {
            this(true);
        }

        protected DescriptorImpl(boolean load) {
            if (load)
                load();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Notify via WebHook";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            // to persist global configuration information,
            // set that to properties and call save().
            WebHookType = formData.getString("webHookType");
            WebHookURL = formData.getString("webHookURL");
            AuthKey = formData.getString("authKey");
            AuthToken = formData.getString("authToken");
            NotifyStart = formData.getBoolean("notifyStart");
            NotifySuccess = formData.getBoolean("notifySuccess");
            NotifyAborted = formData.getBoolean("notifyAborted");
            NotifyNotBuilt = formData.getBoolean("notifyNotBuilt");
            NotifyFailed = formData.getBoolean("notifyFailed");

            System.out.println("configure is called");
            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillWebHookTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Get", "Get");
            items.add("Post", "Post");
            return items;
        }

        //TODO: Add validations to url text boxes .
        //TODO: Add Help links on UI.
        //TODO: Prettify UI.
    }

    public PluginConfig getConfig() {
        PluginConfig config = new PluginConfig(getWebHookType(), getWebHookURL(), getAuthKey(), getAuthToken(),getNotifyStart(),getNotifySuccess(),getNotifyAborted(),getNotifyNotBuilt(),getNotifyFailed());
        return config;
    }

}
