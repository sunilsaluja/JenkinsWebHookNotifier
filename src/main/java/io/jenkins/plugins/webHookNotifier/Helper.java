package io.jenkins.plugins.webHookNotifier;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;

public class Helper {

    private static Helper Instance;
    public static final int MAX_FIELD_LENGTH = 255;
    public static final int MAX_URL_FIELD_LENGTH = 450;

    private Helper() {
    }

    public static Helper getInstance() {
        if (Instance == null) {
            Instance = new Helper();
        }
        return Instance;
    }

    public boolean perform(Run<?, ?> run, FilePath workspace, TaskListener listener,
            boolean disableInprogressNotification, PluginConfig config) {

        BuildState state;
        PrintStream logger = listener.getLogger();
        logger.println("STARTED");
        state = BuildState.STARTED;
        processJenkinsEvent(run, null, listener, state, config);
        Result buildResult = run.getResult();
        if (buildResult == null) {
            // state = BuildState.INPROGRESS;
            // logger.println("STARTED");
            // processJenkinsEvent(run,null, listener, BuildState.INPROGRESS, config);
        } else if (buildResult == Result.SUCCESS) {
            state = BuildState.SUCCESSFUL;
            logger.println("SUCCESSFUL");
            processJenkinsEvent(run, null, listener, state, config);
        } else if (buildResult == Result.UNSTABLE) {
            logger.println("UNSTABLE reported SUCCESSFUL");
            state = BuildState.SUCCESSFUL;
            processJenkinsEvent(run, null, listener, state, config);
        } else if (buildResult == Result.ABORTED) {
            state = BuildState.ABORTED;
            logger.println("ABORTED");
            processJenkinsEvent(run, null, listener, state, config);
        } else if (buildResult.equals(Result.NOT_BUILT)) {
            state = BuildState.NOT_BUILT;
            logger.println("NOT BUILT");
            processJenkinsEvent(run, null, listener, state, config);
        } else {
            state = BuildState.FAILED;
            logger.println("FAILED");
            processJenkinsEvent(run, null, listener, state, config);
        }
        return true;
    }

    private boolean processJenkinsEvent(final Run<?, ?> run, final FilePath workspace, final TaskListener listener,
            final BuildState state, PluginConfig config) {
                if(!doNotify(state, config)){
                    return true;
                }

        PrintStream logger = listener.getLogger();
        //listener.getLogger().println("Hello, i am in process jenkins event!");
        // exit if Jenkins root URL is not configured. Stash run API
        // requires valid link to run in CI system.
        if (getRootUrl() == null) {
            //logger.println("Cannot notify Stash! (Jenkins Root URL not configured)");
            // return true;
        }
        try {
            NotificationResult result = NotifyWebHook(run,state,config);
            if (!result.indicatesSuccess) {
                // after error with URL
                logger.println(result.message);
            }
        } catch (Exception e) {
            logger.println(e.getMessage());
        }
        return true;
    }

    private String getRootUrl() {
        Jenkins instance = Jenkins.getInstance();
        JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();

        if (null == instance) {
            return globalConfig.getUrl();
        }

        return (instance.getRootUrl() != null) ? instance.getRootUrl() : globalConfig.getUrl();
    }

    private NotificationResult NotifyWebHook(final Run<?, ?> build,BuildState state, PluginConfig config) throws Exception {

        if ("Post".equals(config.getWebHookType())) {
            HttpPost request = new HttpPost(buildWebHookURL(config));
            request.addHeader("Content-type", "application/json");
            HttpEntity entity = newStashBuildNotificationEntity( build,state);
            request.setEntity(entity);

            HttpClient client = getHttpClient();
            try {
                client.execute(request);
                return NotificationResult.newSuccess();
            } catch (Exception ex) {
                return NotificationResult.newFailure(ex.getMessage());
            } finally {
                client.getConnectionManager().shutdown();
            }
        } else if ("Get".equals(config.getWebHookType())) {
            HttpGet request = new HttpGet();
            request.setURI(URI.create(buildWebHookURL(config)));
            request.addHeader("Content-type", "application/json");
            HttpClient client = getHttpClient();
            try {
                client.execute(request);
                return NotificationResult.newSuccess();
            } catch (Exception ex) {
                return NotificationResult.newFailure(ex.getMessage());
            } finally {
                client.getConnectionManager().shutdown();
            }
        } else {
            return NotificationResult.newFailure("Invalid Webhook Request Type. == " + config.getWebHookType());
        }
    }

    private HttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setSocketTimeout(60000);
        builder.setDefaultRequestConfig(requestBuilder.build());
        return builder.build();
    }

    private String buildWebHookURL(PluginConfig config) {
        String url = config.getWebHookURL();
        String key = config.getAuthKey();
        String token = config.getAuthToken();
        if (key != null && !"".equals(key) && key.length() != 0) {
            url = url + "?key=" + key;
            if (token != null && !"".equals(token) && token.length() != 0) {
                url = url + "&token=" + token;
            }
        }
        return url;
    }

    private HttpEntity newStashBuildNotificationEntity(final Run<?, ?> run,BuildState state) throws UnsupportedEncodingException {

        AbstractBuild<?,?> build = (AbstractBuild<?, ?>) run;
        StringBuffer message = new StringBuffer();
        message.append(build.getProject().getFullDisplayName());
        message.append(" - ");
        message.append(build.getDisplayName()+" ");
        if(state == BuildState.SUCCESSFUL){
            message.append(state.name()+" ");
            message.append("after ");
            message.append(Integer.toString(Math.round((build.getDuration()/1000/60))));
            message.append(" min");
        }else{
            CauseAction causeAction = build.getAction(CauseAction.class);
            if(causeAction != null)
            {
                Cause scmCause = causeAction.findCause(hudson.model.Cause.class);
                if(scmCause.getClass().getName().equals("hudson.model.Cause$UserIdCause")){
                    message.append(scmCause.getShortDescription());
                }else{
                    message.append("Started!");
                }
            }else{
                message.append("Started!");
            }
        }
        String url = DisplayURLProvider.get().getRunURL(build);
        message.append(" (<").append(url).append("|Open>)");



        //< JOB Name > - #<JOB number> Started by user <UserName> (Open) >> link to job url 
        
        JSONObject json = new JSONObject();
        json.put("text", message.toString());
        return new StringEntity(json.toString(), "UTF-8");
    }

    private boolean doNotify(BuildState state, PluginConfig config){
        switch (state) {
            case STARTED:
                return config.getNotifyStart();
            case SUCCESSFUL:
                return config.getNotifySuccess();
            case ABORTED:
                return config.getNotifyAborted();
            case NOT_BUILT:
                return config.getNotifyNotBuilt();
            case FAILED:
                return config.getNotifyFailed();
            default:
                return false;
            }
    }

}