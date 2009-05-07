package net.sourceforge.jwbf.actions.mediawiki.editing;

import static net.sourceforge.jwbf.actions.mediawiki.MediaWiki.Version.MW1_12;
import static net.sourceforge.jwbf.actions.mediawiki.MediaWiki.Version.MW1_13;
import static net.sourceforge.jwbf.actions.mediawiki.MediaWiki.Version.MW1_14;

import java.io.IOException;
import java.io.StringReader;

import net.sourceforge.jwbf.actions.Post;
import net.sourceforge.jwbf.actions.mediawiki.MediaWiki;
import net.sourceforge.jwbf.actions.mediawiki.util.MWAction;
import net.sourceforge.jwbf.actions.mediawiki.util.SupportedBy;
import net.sourceforge.jwbf.actions.util.ActionException;
import net.sourceforge.jwbf.actions.util.HttpAction;
import net.sourceforge.jwbf.actions.util.ProcessException;
import net.sourceforge.jwbf.bots.MediaWikiBot;
import net.sourceforge.jwbf.contentRep.Userinfo;
import net.sourceforge.jwbf.contentRep.mediawiki.Siteinfo;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

/**
 * Action class using the MediaWiki-API's <a
 * href="http://www.mediawiki.org/wiki/API:Edit_-_Delete">"action=delete"</a>.
 *
 * <p>
 * To allow your bot to delete articles in your MediaWiki add the following line
 * to your MediaWiki's LocalSettings.php:<br>
 *
 * <pre>
 * $wgEnableWriteAPI = true;
 * $wgGroupPermissions['bot']['delete'] = true;
 * </pre>
 *
 * <p>
 * Delete an article with
 * <pre>
 * String name = ...
 * MediaWikiBot bot = ...
 * Siteinfo si = bot.getSiteinfo();
 * Userinfo ui = bot.getUserinfo();
 * bot.performAction(new PostDelete(name, si, ui));
 * </pre>
 *
 * @author Max Gensthaler
 */
@SupportedBy({MW1_12, MW1_13, MW1_14})
public class PostDelete extends MWAction {
	private static final Logger log = Logger.getLogger(PostDelete.class);
	

	private final String title;

	private final GetApiToken token;
	private boolean delToken = true;
	
	/**
	 * Constructs a new <code>PostDelete</code> action.
	 * @param title title of the article to delete
	 * @param si site info object
	 * @param ui user info object
	 * @throws ProcessException on inner problems like a version mismatch
	 */
	public PostDelete(String title, Siteinfo si, Userinfo ui) throws ProcessException {
		super(si.getVersion());
		token = new GetApiToken(GetApiToken.Intoken.DELETE, title, si, ui);
		this.title = title;
		if (title == null || title.length() == 0) {
			throw new IllegalArgumentException("The argument 'title' must not be \"" + String.valueOf(title) + "\"");
		}
		
		if (false == ui.getRights().contains("delete")) {
			throw new ProcessException("The given user doesn't have the rights to delete. Adding '$wgGroupPermissions['bot']['delete'] = true;' to your MediaWiki's LocalSettings.php might remove this problem.");
		}
		
	}
	
	public PostDelete(MediaWikiBot bot, String title) throws ProcessException, ActionException {
		super(bot.getVersion());
		token = new GetApiToken(GetApiToken.Intoken.DELETE, title, bot.getSiteinfo(), bot.getUserinfo());
		this.title = title;
		if (title == null || title.length() == 0) {
			throw new IllegalArgumentException("The argument 'title' must not be null or empty");
		}
		
		if (false == bot.getUserinfo().getRights().contains("delete")) {
			throw new ProcessException("The given user doesn't have the rights to delete. Adding '$wgGroupPermissions['bot']['delete'] = true;' to your MediaWiki's LocalSettings.php might remove this problem.");
		}
		
	}

	/**
	 * @return the delete action
	 */
	private HttpAction getSecondRequest() {
		HttpAction msg = null;
		if (token.getToken() == null || token.getToken().length() == 0) {
			throw new IllegalArgumentException(
					"The argument 'token' must not be \""
							+ String.valueOf(token.getToken()) + "\"");
		}
		if (log.isTraceEnabled()) {
			log.trace("enter PostDelete.generateDeleteRequest(String)");
		}

		String uS = "/api.php" + "?action=delete" + "&title="
				+ MediaWiki.encode(title) + "&token="
				+ MediaWiki.encode(token.getToken()) + "&format=xml";
		if (log.isDebugEnabled()) {
			log.debug("delete url: \"" + uS + "\"");
		}
		msg = new Post(uS);

		
		return msg;
	}



	/**
	 * Deals with the MediaWiki API's response by parsing the provided text.
	 * @param s the answer to the most recently generated MediaWiki API request
	 * @param hm the requestor message
	 * @return empty string
	 */
	@Override
	public String processReturningText(String s, HttpAction hm)
			throws ProcessException {
		super.processReturningText(s, hm);
		
		if (delToken) {
			token.processReturningText(s, hm);
			delToken = false;
		} else {
			
			if (log.isTraceEnabled()) {
				log.trace("enter PostDelete.processAllReturningText(String)");
			}
			if (log.isDebugEnabled()) {
				log.debug("Got returning text: \"" + s + "\"");
			}
			SAXBuilder builder = new SAXBuilder();
			try {
				Document doc = builder.build(new InputSource(
						new StringReader(s)));
				if (false == containsError(doc)) {
					process(doc);
				}
			} catch (JDOMException e) {
				if (s.startsWith("unknown_action:")) {
					log
							.error(
									"Adding '$wgEnableWriteAPI = true;' to your MediaWiki's LocalSettings.php might remove this problem.",
									e);
				} else {
					log.error(e.getMessage(), e);
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			setHasMoreMessages(false);
		}
		
		return "";
	}



	/**
	 * Determines if the given XML {@link Document} contains an error message
	 * which then would printed by the logger.
	 * @param doc XML <code>Document</code>
	 * @throws JDOMException thrown if the document could not be parsed
	 */
	private boolean containsError(Document doc) throws JDOMException {
		Element elem = doc.getRootElement().getChild("error");
		if( elem != null) {
			log.error(elem.getAttributeValue("info"));
			if(elem.getAttributeValue("code").equals("inpermissiondenied")) {
				log.error("Adding '$wgGroupPermissions['bot']['delete'] = true;' to your MediaWiki's LocalSettings.php might remove this problem.");
			}
			return true;
		}
		return false;
	}

	/**
	 * Processing the XML {@link Document} returned from the MediaWiki API.
	 * @param doc XML <code>Document</code>
	 * @throws JDOMException thrown if the document could not be parsed
	 */
	private void process(Document doc) throws JDOMException {
		Element elem = doc.getRootElement().getChild("delete");
		if (elem != null) {
			// process reply for delete request
			if(log.isInfoEnabled()) {
				log.info("Deleted article '" + elem.getAttributeValue("title") + "'" +
					" with reason '" + elem.getAttributeValue("reason") + "'");
			}
		} else {
			log.error("Unknow reply. This is not a reply for a delete action.");
		}
	}
	
	public HttpAction getNextMessage() {
		if (token.hasMoreMessages()) {
			setHasMoreMessages(true);
			return token.getNextMessage();
		}
		return getSecondRequest();
	}
}