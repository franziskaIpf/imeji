/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.session;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.ImejiConfiguration.BROWSE_VIEW;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.util.MaxPlanckInstitutUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.PropertyReader;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation.Page;
import de.mpg.imeji.presentation.lang.InternationalizationBean;
import de.mpg.imeji.presentation.upload.IngestImage;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * The session Bean for imeji.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean
@SessionScoped
public class SessionBean implements Serializable {
  private static final long serialVersionUID = 3367867290955569762L;

  public enum Style {
    NONE, DEFAULT, ALTERNATIVE;
  }

  private Locale locale;
  private User user = null;
  private Page currentPage;
  private List<String> selected;
  private Album activeAlbum;
  private Map<URI, MetadataProfile> profileCached;
  private Map<URI, CollectionImeji> collectionCached;
  private String selectedImagesContext = null;
  private Style selectedCss = Style.NONE;
  private boolean showLogin = false;
  private int numberOfItemsPerPage = 18;
  private int numberOfContainersPerPage = 10;
  private boolean hasUploadRights = false;

  private String applicationUrl;
  private String spaceId;
  private URI selectedSpace;
  private String selectedSpaceLogoURL;
  private String selectedBrowseListView;

  /*
   * Cookies name
   */
  public static final String styleCookieName = "IMEJI_STYLE";
  public static final String langCookieName = "IMEJI_LANG";
  public static final String numberOfItemsPerPageCookieName = "IMEJI_ITEMS_PER_PAGE";
  public static final String numberOfContainersPerPageCookieName = "IMEJI_CONTAINERS_PER_PAGE";
  public static final String browseViewCookieName = "IMEJI_BROWSE_VIEW";

  /*
   * Specific variables for the May Planck Inistute
   */
  public String institute;
  public String instituteId;

  // TODO
  // Provide better handling for ingest image uploader; here temporary code provided in order to
  // fulfill the sprint deadline
  private IngestImage spaceLogoIngestImage;


  /**
   * The session Bean for imeji
   */
  public SessionBean() {
    selected = new ArrayList<String>();
    profileCached = new HashMap<URI, MetadataProfile>();
    collectionCached = new HashMap<URI, CollectionImeji>();
    locale = InternationalizationBean.getUserLocale();
    initCssWithCookie();
    initApplicationUrl();
    initNumberOfItemsPerPageWithCookieOrProperties();
    initNumberOfContainersPerPageWithCookieOrProperties();
    initBrowseViewWithCookieOrConfig();
    institute = findInstitute();
    instituteId = findInstituteId();
  }

  /**
   * Initialize the number of items per page by:<br/>
   * 1- Reading the property<br/>
   * 2- Reading the Cookie<br/>
   * If the cookie is not null, this value is used, otherwise, a new cookie is created with the
   * value in the porperty
   */
  private void initNumberOfItemsPerPageWithCookieOrProperties() {
    this.numberOfItemsPerPage =
        Integer.parseInt(initWithCookieAndProperty(Integer.toString(numberOfItemsPerPage),
            numberOfItemsPerPageCookieName, "imeji.image.list.size"));
  }

  /**
   * Init the default browse view. If a cookie is set, use it, otherwise use config value
   */
  private void initBrowseViewWithCookieOrConfig() {
    this.selectedBrowseListView =
        CookieUtils.readNonNull(browseViewCookieName, Imeji.CONFIG.getDefaultBrowseView());
  }

  /**
   * Initialize the number of items per page by:<br/>
   * 1- Reading the property<br/>
   * 2- Reading the Cookie<br/>
   * If the cookie is not null, this value is used, otherwise, a new cookie is created with the
   * value in the porperty
   */
  private void initNumberOfContainersPerPageWithCookieOrProperties() {
    this.numberOfContainersPerPage =
        Integer.parseInt(initWithCookieAndProperty(Integer.toString(numberOfContainersPerPage),
            numberOfContainersPerPageCookieName, "imeji.container.list.size"));
  }

  /**
   * Initialize the property by:<br/>
   * 1- Reading the property file<br/>
   * 2- Reading the Cookie<br/>
   * If the cookie is not null, this value is used, otherwise, a new cookie is created with the
   * value from the property file
   *
   * @param value
   * @param cookieName
   * @param propertyName
   * @return
   */
  private String initWithCookieAndProperty(String value, String cookieName, String propertyName) {
    try {
      // First read in the property
      value = PropertyReader.getProperty(propertyName);
    } catch (NumberFormatException | IOException | URISyntaxException e) {
      Logger.getLogger(SessionBean.class)
          .error("There had been some initWithCookieAndProperty issues.", e);
    }
    // Second, Read the cookie and set a default value if null
    return CookieUtils.readNonNull(cookieName, value);
  }

  /**
   * Initialize the CSS value with the Cookie value
   */
  private void initCssWithCookie() {
    selectedCss = Style.valueOf(CookieUtils.readNonNull(styleCookieName, Style.NONE.name()));
  }

  /**
   * Return the version of the software
   *
   * @return
   */
  public String getVersion() {
    return PropertyReader.getVersion();
  }

  /**
   * Return the name of the current application (defined in the property)
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getInstanceName() {
    try {
      return Imeji.CONFIG.getInstanceName();
    } catch (Exception e) {
      return "imeji";
    }
  }

  /**
   * Read application URL from the imeji properties
   */
  private void initApplicationUrl() {
    try {
      applicationUrl = StringHelper.normalizeURI(PropertyReader.getProperty("imeji.instance.url"));
    } catch (Exception e) {
      applicationUrl = "http://localhost:8080/imeji";
    }
  }

  public String getApplicationUrl() {
    return applicationUrl;
  }

  /**
   * Getter
   *
   * @return
   */
  public Locale getLocale() {
    return this.locale;
  }

  /**
   * Setter
   *
   * @param userLocale
   */
  public void setLocale(final Locale userLocale) {
    this.locale = userLocale;
  }

  /**
   * Get the context of the images (item, collection, album)
   *
   * @return
   */
  public String getSelectedImagesContext() {
    return selectedImagesContext;
  }

  /**
   * setter
   *
   * @param selectedImagesContext
   */
  public void setSelectedImagesContext(String selectedImagesContext) {
    this.selectedImagesContext = selectedImagesContext;
  }

  public void reloadUser() throws Exception {
    if (user != null) {
      UserController c = new UserController(user);
      user = c.retrieve(user.getId());
      checkIfHasUploadRights();
    }
  }

  /**
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * @param user the user to set
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * getter
   *
   * @return
   */
  public Page getCurrentPage() {
    return currentPage;
  }

  /**
   * setter
   *
   * @param currentPage
   */
  public void setCurrentPage(Page currentPage) {
    this.currentPage = currentPage;
  }

  /**
   * getter
   *
   * @return
   */
  public List<String> getSelected() {
    return selected;
  }

  /**
   * setter
   *
   * @param selected
   */
  public void setSelected(List<String> selected) {
    this.selected = selected;
  }

  /**
   * Return the number of item selected
   *
   * @return
   */
  public int getSelectedSize() {
    return selected.size();
  }


  /**
   * setter
   *
   * @param activeAlbum
   */
  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }

  /**
   * getter
   *
   * @return
   */
  public Album getActiveAlbum() {
    if (activeAlbum != null && (!AuthUtil.staticAuth().read(getUser(), activeAlbum.getId())
        || !AuthUtil.staticAuth().create(getUser(), activeAlbum.getId()))) {
      setActiveAlbum(null);
    }
    return activeAlbum;
  }

  /**
   * setter
   *
   * @return
   */
  public String getActiveAlbumId() {
    return ObjectHelper.getId(activeAlbum.getId());
  }

  /**
   * getter
   *
   * @return
   */
  public int getActiveAlbumSize() {
    return activeAlbum.getImages().size();
  }

  /**
   * Getter
   *
   * @return
   */
  public Map<URI, MetadataProfile> getProfileCached() {
    return profileCached;
  }

  /**
   * Setter
   *
   * @param profileCached
   */
  public void setProfileCached(Map<URI, MetadataProfile> profileCached) {
    this.profileCached = profileCached;
  }

  /**
   * @return the collectionCached
   */
  public Map<URI, CollectionImeji> getCollectionCached() {
    return collectionCached;
  }

  /**
   * @param collectionCached the collectionCached to set
   */
  public void setCollectionCached(Map<URI, CollectionImeji> collectionCached) {
    this.collectionCached = collectionCached;
  }

  /**
   * Check if the selected CSS is correct according to the configuration value. If errors are found,
   * then change the selected CSS
   *
   * @param defaultCss - the value of the default css in the config
   * @param alternativeCss - the value of the alternative css in the config
   */
  public void checkCss(String defaultCss, String alternativeCss) {
    if (selectedCss == Style.ALTERNATIVE && (alternativeCss == null || "".equals(alternativeCss))) {
      // alternative css doesn't exist, therefore set to default
      selectedCss = Style.DEFAULT;
    }
    if (selectedCss == Style.DEFAULT && (defaultCss == null || "".equals(defaultCss))) {
      // default css doesn't exist, therefore set to none
      selectedCss = Style.NONE;
    }
    if (selectedCss == Style.NONE && defaultCss != null && !"".equals(defaultCss)) {
      // default css exists, therefore set to default
      selectedCss = Style.DEFAULT;
    }
  }

  /**
   * Get the the selected {@link Style}
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getSelectedCss() {
    return selectedCss.name();
  }

  /**
   * Toggle the selected css
   *
   * @return
   */
  public void toggleCss() {
    selectedCss = selectedCss == Style.DEFAULT ? Style.ALTERNATIVE : Style.DEFAULT;
    CookieUtils.updateCookieValue(styleCookieName, selectedCss.name());
  }



  public boolean isShowLogin() {
    return showLogin;
  }

  public void setShowLogin(boolean showLogin) {
    this.showLogin = showLogin;
  }

  /**
   * @return the numberOfItemsPerPage
   */
  public int getNumberOfItemsPerPage() {
    return numberOfItemsPerPage;
  }

  /**
   * @param numberOfItemsPerPage the numberOfItemsPerPage to set
   */
  public void setNumberOfItemsPerPage(int numberOfItemsPerPage) {
    this.numberOfItemsPerPage = numberOfItemsPerPage;
  }

  /**
   * @return the numberOfContainersPerPage
   */
  public int getNumberOfContainersPerPage() {
    return numberOfContainersPerPage;
  }

  /**
   * @param numberOfContainersPerPage the numberOfContainersPerPage to set
   */
  public void setNumberOfContainersPerPage(int numberOfContainersPerPage) {
    this.numberOfContainersPerPage = numberOfContainersPerPage;
  }

  /**
   * Return the Institute of the current {@link User} according to his IP. IMPORTANT: works only for
   * Max Planck Institutes IPs.
   *
   * @return
   */
  public String getInstituteNameByIP() {
    if (StringUtils.isEmpty(institute)) {
      return "unknown";
    }
    return institute;
  }

  /**
   * Return the Institute of the current {@link User} according to his IP. IMPORTANT: works only for
   * Max Planck Institutes IPs.
   *
   * @return
   */
  public String getInstituteIdByIP() {
    if (StringUtils.isEmpty(institute)) {
      return "unknown";
    }
    return instituteId;
  }

  /**
   * Return the suffix of the email of the user
   *
   * @return
   */
  public String getInstituteByUser() {
    if (user != null) {
      return user.getEmail().split("@")[1];
    }
    return "";
  }

  /**
   * Find the Name of the Institute of the current user
   */
  public String findInstitute() {
    if (institute != null) {
      return institute;
    }
    return MaxPlanckInstitutUtils.getInstituteNameForIP(readUserIp());
  }

  /**
   * Find the Name of the Institute of the current user
   */
  public String findInstituteId() {
    if (instituteId != null) {
      return instituteId;
    }
    return MaxPlanckInstitutUtils.getInstituteIdForIP(readUserIp());
  }

  /**
   * Read the IP of the current User
   *
   * @return
   */
  private String readUserIp() {
    HttpServletRequest request =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    String ipAddress = request.getHeader("X-FORWARDED-FOR");
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }
    if (ipAddress != null && ipAddress.split(",").length > 1) {
      ipAddress = ipAddress.split(",")[0];
    }
    return ipAddress;
  }

  public void setSpaceId(String spaceIdString) {
    this.spaceId = spaceIdString;
    if (!isNullOrEmpty(spaceIdString)) {
      SpaceController sc = new SpaceController();
      try {
        Space selectedSpace = sc.retrieveSpaceByLabel(spaceIdString, this.user);
        if (selectedSpace != null) {
          this.selectedSpace = selectedSpace.getId();
          this.selectedSpaceLogoURL = String.valueOf(selectedSpace.getLogoUrl());
          logoutFromSpot();
        } else {
          this.selectedSpace = null;
          this.spaceId = "";
          this.selectedSpaceLogoURL = "";
        }
      } catch (ImejiException e) {
        this.selectedSpace = null;
        this.spaceId = "";
        this.selectedSpaceLogoURL = "";
      }
    } else {
      this.selectedSpace = null;
      this.spaceId = "";
      this.selectedSpaceLogoURL = "";
    }
  }

  public String getSpaceId() {
    return this.spaceId;
  }

  public String getSelectedSpaceString() {
    if (this.selectedSpace != null) {
      return this.selectedSpace.toString();
    }
    return "";
  }

  public URI getSelectedSpace() {
    return this.selectedSpace;
  }

  public String getSelectedSpaceLogoURL() {
    return this.selectedSpaceLogoURL;
  }

  public IngestImage getSpaceLogoIngestImage() {
    return spaceLogoIngestImage;
  }

  public void setSpaceLogoIngestImage(IngestImage spaceLogoIngestImage) {
    this.spaceLogoIngestImage = spaceLogoIngestImage;
  }

  public String getPrettySpacePage(String prettyPage) {
    return getPrettySpacePage(prettyPage, spaceId);
  }

  public static String getPrettySpacePage(String prettyPage, String space) {
    if (isNullOrEmpty(space)) {
      return prettyPage;
    }
    return prettyPage.replace("pretty:", "pretty:space_");
  }

  /**
   * Logout and redirect to the home page
   *
   * @throws IOException
   */
  private void logoutFromSpot() {
    if (getUser() != null && !getUser().isAdmin()) {
      setUser(null);
      setShowLogin(false);
    }
  }

  public String getSelectedBrowseListView() {
    return selectedBrowseListView;
  }

  public void setSelectedBrowseListView(String selectedBrowseListView) {
    this.selectedBrowseListView = selectedBrowseListView;
  }

  public void toggleBrowseView() {
    selectedBrowseListView =
        selectedBrowseListView.equals(ImejiConfiguration.BROWSE_VIEW.LIST.name())
            ? BROWSE_VIEW.THUMBNAIL.name() : BROWSE_VIEW.LIST.name();
    CookieUtils.updateCookieValue(browseViewCookieName, selectedBrowseListView);
  }

  /**
   * True if the current user has either right to create a collection or to upload items in at least
   * one collection
   *
   * @return
   */
  public boolean isHasUploadRights() {
    return hasUploadRights;
  }

  /**
   * Check and set isHasUploadRights
   */
  public void checkIfHasUploadRights() {
    if (user.isAllowedToCreateCollection()) {
      hasUploadRights = true;
      return;
    }
    List<String> collectionUris =
        new CollectionController().search(null, null, -1, 0, user, spaceId).getResults();
    for (String uri : collectionUris) {
      if (AuthUtil.staticAuth().createContent(user, uri)) {
        hasUploadRights = true;
        return;
      }
    }
    hasUploadRights = false;
  }
}
