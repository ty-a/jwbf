/*
 * Copyright 2007 Justus Bisser.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors:
 * Thomas Stock
 */
package net.sourceforge.jwbf.mediawiki.actions.editing;

import java.util.Deque;

import com.google.common.collect.Queues;
import net.sourceforge.jwbf.core.actions.Post;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.ApiRequestBuilder;
import net.sourceforge.jwbf.mediawiki.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.editing.GetApiToken.Intoken;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import net.sourceforge.jwbf.mediawiki.contentRep.SimpleFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * To allow your bot to upload media in your MediaWiki. Add at least the following line to your
 * MediaWiki's LocalSettings.php:<br>
 * <pre>
 * $wgEnableUploads = true;
 * </pre>
 *
 * @author Justus Bisser
 * @author Thomas Stock
 * @see <a href="http://www.mediawiki.org/wiki/Help:Configuration_settings#Uploads" >Upload
 * Config</a>
 */
public class FileUpload extends MWAction {

  private static final Logger log = LoggerFactory.getLogger(FileUpload.class);

  private final Deque<HttpAction> actions;
  private UploadAction actionHandler;

  public FileUpload(final SimpleFile simpleFile, MediaWikiBot bot) {
    if (!simpleFile.getFile().isFile() || !simpleFile.getFile().canRead()) {
      throw new ActionException("no such file " + simpleFile.getFile());
    }
    if (!bot.isLoggedIn()) {
      throw new ActionException("Please login first");
    }

    if (!simpleFile.getFile().exists()) {
      throw new IllegalStateException("file not found" + simpleFile.getFile());
    }
    actionHandler = new ApiUpload(bot, simpleFile);
    actions = actionHandler.getActions();

  }

  public FileUpload(MediaWikiBot bot, String filename) {
    this(new SimpleFile(filename), bot);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpAction getNextMessage() {
    return actions.pop();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasMoreMessages() {
    return !actions.isEmpty();
  }

  @Override
  public String processReturningText(String xml, HttpAction hm) {
    return actionHandler.handleResponse(xml, hm);
  }

  private static class ApiUpload implements UploadAction {
    private final Deque<HttpAction> actions = Queues.newArrayDeque();
    private final SimpleFile simpleFile;
    private GetApiToken uploadTokenAction;

    public ApiUpload(MediaWikiBot bot, SimpleFile simpleFile) {
      this.simpleFile = simpleFile;
    }

    @Override
    public Deque<HttpAction> getActions() {
      uploadTokenAction = new GetApiToken(Intoken.EDIT, simpleFile.getPath());
      actions.add(uploadTokenAction.popAction());
      return actions;
    }

    @Override
    public String handleResponse(String xml, HttpAction hm) {
      if (uploadTokenAction != null) {
        uploadTokenAction.processReturningText(xml, hm);
        Post upload = new ApiRequestBuilder() //
            .action("upload") //
            .formatXml() //
            .param(uploadTokenAction.get().urlEncodedToken()) //
            .param("filename", MediaWiki.urlEncode(simpleFile.getTitle())) //
            .param("ignorewarnings", true) //
            .buildPost() //
            .postParam("file", simpleFile.getFile()) //
            ;
        actions.add(upload);
        uploadTokenAction = null; // XXX
      }
      // file upload requires enabled uploads, upload rights and filesystem permisions
      return xml;
    }
  }

  private interface UploadAction {

    Deque<HttpAction> getActions();

    String handleResponse(String xml, HttpAction hm);
  }

}
