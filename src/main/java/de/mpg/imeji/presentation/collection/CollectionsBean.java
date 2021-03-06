/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.sparql.pfunction.library.container;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.presentation.beans.SuperContainerBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ListUtils;

/**
 * Bean for the collections page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CollectionsBean extends SuperContainerBean<CollectionListItem> {

  /**
   * The comment required to discard a {@link container}
   */
  private String discardComment = "";

  /**
   * Bean for the collections page
   */
  public CollectionsBean() {
    super();
    this.sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  @Override
  public String getNavigationString() {
    return sb.getPrettySpacePage("pretty:collections");
  }

  @Override
  public List<CollectionListItem> retrieveList(int offset, int limit) throws Exception {
    CollectionController controller = new CollectionController();
    Collection<CollectionImeji> collections = new ArrayList<CollectionImeji>();
    search(offset, limit);
    setTotalNumberOfRecords(searchResult.getNumberOfRecords());
    collections = controller.retrieveBatchLazy(searchResult.getResults(), -1, offset, sb.getUser());
    return ListUtils.collectionListToListItem(collections, sb.getUser());
  }


  @Override
  public String selectAll() {
    // Not implemented
    return "";
  }

  @Override
  public String selectNone() {
    // Not implemented
    return "";
  }

  /**
   * getter
   *
   * @return
   */
  public String getDiscardComment() {
    return discardComment;
  }

  /**
   * setter
   *
   * @param discardComment
   */
  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.COLLECTION_ITEMS.name();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.SuperContainerBean#search(de.mpg.imeji.logic.search.vo.
   * SearchQuery , de.mpg.imeji.logic.search.vo.SortCriterion)
   *
   * @param searchQuery
   *
   * @param sortCriterion
   *
   * @return
   */
  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    CollectionController controller = new CollectionController();
    return controller.search(searchQuery, sortCriterion, limit, offset, sb.getUser(),
        sb.getSelectedSpaceString());
  }

  public String getTypeLabel() {
    return Imeji.RESOURCE_BUNDLE.getLabel("type_" + getType().toLowerCase(), sb.getLocale());
  }


}
