package net.sourceforge.jwbf.mediawiki.actions.queries;

import static net.sourceforge.jwbf.TestHelper.getRandom;

import com.google.common.collect.ImmutableList;
import net.sourceforge.jwbf.GAssert;
import net.sourceforge.jwbf.TestHelper;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.BotFactory;
import net.sourceforge.jwbf.mediawiki.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.live.AbstractMediaWikiBotTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Stock
 */
public class ImageUsageTitlesLiveIntegTest extends AbstractMediaWikiBotTest {

  private static final Logger log = LoggerFactory.getLogger(ImageUsageTitlesLiveIntegTest.class);

  private static final int limit = 55;
  public static final String IMAGE_NAME = "Image:Any.gif";

  @Test
  public final void imageUsageLatest() {
    bot(BotFactory.getMediaWikiBot(Version.getLatest(), true));
    test();
  }

  private void test() {
    // GIVEN
    ImmutableList<String> expectedTitles = TestHelper.createNames("TitleWithImg", limit);

    ImmutableList<String> initPageTitles = new ImageUsageTitles(bot(), IMAGE_NAME).getCopyOf(limit);
    ImmutableList<String> pageTitles;
    if (initPageTitles.size() < limit) {
      prepare(expectedTitles);
      pageTitles = new ImageUsageTitles(bot(), IMAGE_NAME).getCopyOf(limit);
    } else {
      pageTitles = initPageTitles;
    }

    // THEN
    GAssert.assertEquals(expectedTitles, pageTitles);

  }

  private void prepare(ImmutableList<String> expectedTitles) {
    log.info("begin prepare");
    for (String name : expectedTitles) {
      Article a = new Article(bot, name);
      a.setText("Hello [[" + IMAGE_NAME + "]] - " + getRandom(10));
      a.save();
    }
  }

}
