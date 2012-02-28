package hudson.plugins.memegen;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Asgeir Storesund Nilsen
 *
 */
public class MemeNotifier extends Notifier {

	private static final Logger LOGGER = Logger.getLogger(MemeNotifier.class.getName());
	private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	public boolean memeEnabledFailure;
	public boolean memeEnabledSuccess;
	public boolean memeEnabledAlways;

	@DataBoundConstructor
	public MemeNotifier(boolean memeEnabledFailure, boolean memeEnabledSuccess, boolean memeEnabledAlways) {
		//System.err.println("MemeNotifier called with args: "+memeUsername+", "+memePassword+", "+enableFailure+", "+enableSucceed+", "+enableAlways);
		this.memeEnabledAlways = memeEnabledAlways;
		this.memeEnabledSuccess = memeEnabledSuccess;
		this.memeEnabledFailure = memeEnabledFailure;
		System.err.println("Params: " + memeEnabledAlways + ", " + memeEnabledSuccess + ", " + memeEnabledFailure);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	private void generate(AbstractBuild build, BuildListener listener) {
		System.err.println("generate() Auth: " + DESCRIPTOR.memeUsername + ", " + DESCRIPTOR.memePassword);

		listener.getLogger().println("Generating Meme with account " + DESCRIPTOR.memeUsername);
		final String buildId = build.getProject().getDisplayName() + " " + build.getDisplayName();
		MemegeneratorAPI memegenAPI = new MemegeneratorAPI(DESCRIPTOR.memeUsername, DESCRIPTOR.memePassword);
		boolean memeResult;
		try {
			Meme meme = MemeFactory.getMeme(build);
			memeResult = memegenAPI.instanceCreate(meme);

			if (memeResult) {
				listener.getLogger().println("Meme: " + meme.getImageURL());
				build.setDescription("<img class=\"meme\" src=\"" + meme.getImageURL() + "\" />");
				AbstractProject proj = build.getProject();
				String desc = proj.getDescription();
				desc = desc.replaceAll("<img class=\"meme\"[^>]+>", "");
				desc += "<img class=\"meme\" src=\"" + meme.getImageURL() + "\" />";
				proj.setDescription(desc);
			} else {
				listener.getLogger().println("Sorry, couldn't create a Meme - check the logs for more detail");
			}
		} catch (IOException ie) {
			LOGGER.log(Level.WARNING, "{0}{1}", new Object[]{"Meme generation failed: ", ie.getMessage()});
			listener.getLogger().println("Sorry, couldn't create a Meme - check the logs for more detail");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "{0}{1}", new Object[]{"Meme generation failed: ", e.getMessage()});
			listener.getLogger().println("Sorry, couldn't create a Meme - check the logs for more detail");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
		BuildListener listener) throws InterruptedException, IOException {

		System.err.println("perform(): " + memeEnabledAlways + ", " + memeEnabledFailure);
		if (memeEnabledAlways) {
			listener.getLogger().println("Generating Meme...");
			generate(build, listener);
		} else if (memeEnabledSuccess && build.getResult() == Result.SUCCESS) {
			AbstractBuild prevBuild = build.getPreviousBuild();
			if (prevBuild.getResult() == Result.FAILURE) {
				listener.getLogger().println("Build has returned to successful, generating Meme...");
				generate(build, listener);
			}
		} else if (memeEnabledFailure && build.getResult() == Result.FAILURE) {
			listener.getLogger().println("Build failure, generating Meme...");
			generate(build, listener);
		}
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public String memeUsername;
		public String memePassword;

		public static ArrayList<Meme> memes = new ArrayList<Meme>();

		public DescriptorImpl() {
			super(MemeNotifier.class);
			load();
		}
		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			memeUsername = req.getParameter("memeUsername");
			memePassword = req.getParameter("memePassword");

			for (Object data : getArray(json.get("memes"))) {
				Meme m = req.bindJSON(Meme.class, (JSONObject) data);
				memes.add(m);
			}
			save();
			return super.configure(req, json);
		}

		public static JSONArray getArray(Object data) {
			JSONArray result;
			if (data instanceof JSONArray) {
				result = (JSONArray) data;
			} else {
				result = new JSONArray();
				if (data != null) {
					result.add(data);
				}
			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Meme Generator";
		}
	}
}
