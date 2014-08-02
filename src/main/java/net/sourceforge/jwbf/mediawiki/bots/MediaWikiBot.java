package net.sourceforge.jwbf.mediawiki.bots;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URL;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sourceforge.jwbf.core.Transform;
import net.sourceforge.jwbf.core.actions.ContentProcessable;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.bots.HttpBot;
import net.sourceforge.jwbf.core.bots.WikiBot;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.core.contentRep.Userinfo;
import net.sourceforge.jwbf.mediawiki.MediaWiki;
import net.sourceforge.jwbf.mediawiki.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.editing.GetRevision;
import net.sourceforge.jwbf.mediawiki.actions.editing.PostDelete;
import net.sourceforge.jwbf.mediawiki.actions.editing.PostModifyContent;
import net.sourceforge.jwbf.mediawiki.actions.login.PostLogin;
import net.sourceforge.jwbf.mediawiki.actions.meta.GetUserinfo;
import net.sourceforge.jwbf.mediawiki.actions.meta.GetVersion;
import net.sourceforge.jwbf.mediawiki.actions.meta.Siteinfo;
import net.sourceforge.jwbf.mediawiki.contentRep.LoginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class helps you to interact with each <a href="http://www.mediawiki.org"
 * target="_blank">MediaWiki</a>. This class offers a <b>basic set</b> of methods which are defined
 * in the package net.sourceforge.jwbf.actions.mw.* How to use:
 * <pre>
 * MediaWikiBot b = new MediaWikiBot(&quot;http://yourwiki.org&quot;);
 * b.login(&quot;Username&quot;, &quot;Password&quot;);
 * System.out.println(b.readContent(&quot;Main Page&quot;).getText());
 * </pre>
 * <b>How to find the correct wikiurl</b> <p> The correct wikiurl is sometimes not easy to find,
 * because some wikiadmis uses url rewriting rules. In this cases the correct url is the one, which
 * gives you access to <code>api.php</code>. E.g. Compare
 * <pre>
 * http://www.mediawiki.org/wiki/api.php
 * http://www.mediawiki.org/w/api.php
 * </pre>
 * Thus the correct wikiurl is: <code>http://www.mediawiki.org/w/</code> </p>
 *
 * @author Thomas Stock
 * @author Tobias Knerr
 * @author Justus Bisser
 */
public class MediaWikiBot implements WikiBot {

  private static final Logger log = LoggerFactory.getLogger(MediaWikiBot.class);

  private LoginData login = null;

  private Version version = null;
  private Userinfo ui = null;

  private boolean loginChangeUserInfo = false;
  private boolean loginChangeVersion = false;

  @Inject
  private HttpBot bot;

  private HttpActionClient client;

  /**
   * These chars are not allowed in article names.
   */
  public static final char[] INVALID_LABEL_CHARS = "[]{}<>|".toCharArray();
  private static final int DEFAULT_READ_PROPERTIES =
      GetRevision.CONTENT | GetRevision.COMMENT | GetRevision.USER | GetRevision.TIMESTAMP |
          GetRevision.IDS |
          GetRevision.FLAGS;

  /**
   * use this constructor, if you want to work with IoC.
   */
  public MediaWikiBot() {

  }

  /**
   * @param u wikihosturl like "http://www.mediawiki.org/w/"
   */
  public MediaWikiBot(final URL u) {
    this(HttpActionClient.of(u));
  }

  public MediaWikiBot(final HttpActionClient client) {
    this.client = client;
    bot = new HttpBot(client);
  }

  /**
   * @param url wikihosturl like "http://www.mediawiki.org/w/"
   * @throws IllegalArgumentException if param url does not represent a well-formed url
   */

  public MediaWikiBot(final String url) {
    if (!(url.endsWith(".php") || url.endsWith("/"))) {
      throw new IllegalArgumentException("(" + url + ") url must end with slash or .php");
    }
    this.client = HttpActionClient.of(url);
    bot = new HttpBot(client);
  }

  /**
   * @param url               wikihosturl like "http://www.mediawiki.org/w/"
   * @param testHostReachable if true, test if host reachable
   */
  public MediaWikiBot(URL url, boolean testHostReachable) {
    bot = new HttpBot(client);
    if (testHostReachable) {
      HttpBot.getPage(client);
    }
  }

  /**
   * Performs a Login.
   *
   * @param username the username
   * @param passwd   the password
   * @param domain   login domain (Special for LDAPAuth extention to authenticate against LDAP
   *                 users)
   * @see PostLogin
   */
  public void login(final String username, final String passwd, final String domain) {

    this.login = getPerformedAction(new PostLogin(username, passwd, domain)).getLoginData();
    loginChangeUserInfo = true;
    if (getVersion() == Version.UNKNOWN) {
      loginChangeVersion = true;
    }

  }

  /**
   * Performs a Login. Actual old cookie login works right, because is pending on {@link
   * #writeContent(net.sourceforge.jwbf.core.contentRep.SimpleArticle)}
   *
   * @param username the username
   * @param passwd   the password
   * @see PostLogin
   */
  @Override
  public void login(final String username, final String passwd) {
    login(username, passwd, null);
  }

  /**
   * @param name       of article in a mediawiki like "Main Page"
   * @param properties {@link GetRevision}
   * @return a content representation of requested article, never null
   * @see GetRevision
   */
  public synchronized Article getArticle(final String name, final int properties) {
    return new Article(this, readData(properties, name));
  }

  /**
   * @deprecated use {@link #readData(int, String)}
   */
  @Override
  public synchronized SimpleArticle readData(final String name, final int properties) {
    return getPerformedAction(new GetRevision(getVersion(), name, properties)).getArticle();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized SimpleArticle readData(final int properties, final String name) {
    return getPerformedAction(new GetRevision(getVersion(), name, properties)).getArticle();
  }

  @Override
  public ImmutableList<SimpleArticle> readData(int properties, String... names) {
    return readData(properties, ImmutableList.copyOf(names));
  }

  @Override
  public ImmutableList<SimpleArticle> readData(String... names) {
    return readData(ImmutableList.copyOf(names));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SimpleArticle readData(String name) {
    return readData(DEFAULT_READ_PROPERTIES, name);
  }

  @Override
  public ImmutableList<SimpleArticle> readData(int properties, ImmutableList<String> names) {
    // FIXME
    return Transform.the(names, TO_FAKE_ARTICLES);
  }

  @Override
  public ImmutableList<SimpleArticle> readData(ImmutableList<String> names) {
    return readData(DEFAULT_READ_PROPERTIES, names);
  }

  /**
   * @param name of article in a mediawiki like "Main Page"
   * @return a content representation of requested article, never null
   * @see GetRevision
   */
  public synchronized Article getArticle(final String name) {
    return getArticle(name, DEFAULT_READ_PROPERTIES);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void writeContent(final SimpleArticle simpleArticle) {
    if (!isLoggedIn()) {
      throw new ActionException("Please login first");
    }

    SimpleArticle nonNullArticle =
        Preconditions.checkNotNull(simpleArticle, "content must not be null");
    checkTitle(nonNullArticle.getTitle());

    getPerformedAction(new PostModifyContent(this, simpleArticle));
    if (nonNullArticle.getText().trim().length() < 1) {
      throw new RuntimeException("Content is empty, still written");
    }
  }

  static Optional<String> checkTitle(String title) {
    for (char invChar : INVALID_LABEL_CHARS) {
      if (title.contains(invChar + "")) {
        throw new ActionException(
            "Invalid character \"" + invChar + "\" in label \"" + title + "\"");
      }
    }
    return Optional.of(title);
  }

  /**
   * @return true if
   */
  public final boolean isLoggedIn() {
    return login != null && login.isLoggedIn();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Userinfo getUserinfo() {
    if (ui == null || loginChangeUserInfo) {
      ui = getPerformedAction(GetUserinfo.class);
      loginChangeUserInfo = false;
    }
    return ui;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String title) {
    getPerformedAction(new PostDelete(getUserinfo(), title));
  }

  /**
   * deletes an article with a reason
   */
  public void delete(String title, String reason) {
    getPerformedAction(new PostDelete(getUserinfo(), title, reason));
  }

  /**
   * @deprecated use {@link #getPerformedAction(ContentProcessable)} instead
   */
  @Deprecated
  synchronized String performAction(ContentProcessable a) {
    if (a.isSelfExecuter()) {
      throw new ActionException(
          "this is a selfexcecuting action, " + "please do not perform this action manually");
    }
    return bot().performAction(a);
  }

  public synchronized <T extends ContentProcessable> T getPerformedAction(T answer) {
    // TODO transform to throttled
    performAction(answer);
    return answer;
  }

  public synchronized <T extends ContentProcessable> T getPerformedAction(Class<T> clazz) {
    T answer;
    try {
      answer = clazz.newInstance();
      return getPerformedAction(answer);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private HttpBot bot() {
    if (bot == null) {
      throw new IllegalStateException("please use another constructor or inject " + //
          HttpBot.class.getCanonicalName());
    }
    return bot;
  }

  /**
   * @see #getSiteinfo()
   */
  @Nonnull
  public Version getVersion() {
    if (version == null || loginChangeVersion) {
      GetVersion gs = getPerformedAction(GetVersion.class);
      version = gs.getVersion();
      loginChangeVersion = false;
      log.debug("Version is: {}", version.name());
    }
    return version;
  }

  /**
   * @return a on problems with http, cookies and io
   * @see Siteinfo
   */
  @Nonnull
  public Siteinfo getSiteinfo() {
    // TODO cache value see getVersion
    return getPerformedAction(Siteinfo.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getWikiType() {
    return MediaWiki.class.getSimpleName() + " " + getVersion();
  }

  // FIXME remove this
  private static Function<String, SimpleArticle> TO_FAKE_ARTICLES = new Function<String, SimpleArticle>() {
    @Nullable
    @Override
    public SimpleArticle apply(@Nullable String s) {
      SimpleArticle simpleArticle = new SimpleArticle();
      simpleArticle.setTitle(s);
      return simpleArticle;
    }
  };

}
