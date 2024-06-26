package net.plavcak.jenkins.plugins.scmskip;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

public class SCMSkipBuildWrapper extends BuildWrapper {

    private static final Logger LOGGER = Logger.getLogger(SCMSkipBuildWrapper.class.getName());

    private SCMSkipMatcher skipMatcher;
    private boolean deleteBuild;
    private String skipPattern;
    private boolean doStopWorkflowRun;

    @DataBoundConstructor
    public SCMSkipBuildWrapper(boolean deleteBuild, String skipPattern, boolean doStopWorkflowRun) {
        this.deleteBuild = deleteBuild;
        this.skipPattern = skipPattern;
        this.doStopWorkflowRun = doStopWorkflowRun;
        if (this.skipPattern == null) {
            this.skipPattern = SCMSkipConstants.DEFAULT_PATTERN;
        }
        this.skipMatcher = new SCMSkipMatcher(getSkipPattern());
    }

    public boolean isDeleteBuild() {
        return deleteBuild;
    }

    @DataBoundSetter
    public void setDeleteBuild(boolean deleteBuild) {
        this.deleteBuild = deleteBuild;
    }

    public String getSkipPattern() {
        if (StringUtils.isEmpty(this.skipPattern)) {
            return getDescriptor().getSkipPattern();
        }
        return skipPattern;
    }

    @DataBoundSetter
    public void setSkipPattern(String skipPattern) {
        this.skipPattern = skipPattern;
        this.skipMatcher.setPattern(this.skipPattern);
    }

    public boolean isDoStopWorkflowRun() {
        return doStopWorkflowRun;
    }

    @DataBoundSetter
    public void setDoStopWorkflowRun(boolean doStopWorkflowRun) {
        this.doStopWorkflowRun = doStopWorkflowRun;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, FlowInterruptedException {
        if (SCMSkipTools.inspectChangeSetAndCause(build, skipMatcher, listener)) {
            SCMSkipTools.tagRunForDeletion(build, deleteBuild);

            try {
                SCMSkipTools.stopBuild(build, doStopWorkflowRun);
            } catch (AbortException | FlowInterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "SCM Skip Build Wrapper", e);
            }
        } else {
            SCMSkipTools.tagRunForDeletion(build, false);
        }

        return new Environment() {};
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        private String skipPattern = SCMSkipConstants.DEFAULT_PATTERN;

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "SCM Skip";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            req.bindJSON(this, json.getJSONObject("scmSkip"));
            save();
            return true;
        }

        public String getSkipPattern() {
            return skipPattern;
        }

        @DataBoundSetter
        public void setSkipPattern(String skipPattern) {
            this.skipPattern = skipPattern;
        }
    }
}
