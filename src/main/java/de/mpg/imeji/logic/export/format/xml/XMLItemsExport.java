/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export.format.xml;

import java.io.OutputStream;
import java.util.Collection;

import de.mpg.imeji.logic.export.format.XMLExport;
import de.mpg.imeji.logic.ingest.jaxb.JaxbUtil;
import de.mpg.imeji.logic.ingest.vo.Items;
import de.mpg.imeji.logic.resource.controller.ItemController;
import de.mpg.imeji.logic.resource.vo.Item;
import de.mpg.imeji.logic.resource.vo.User;
import de.mpg.imeji.logic.search.SearchResult;

/**
 * Export the information for the ingest issue
 * 
 * @author hnguyen
 */
public class XMLItemsExport extends XMLExport {
  @Override
  public void init() {
    // No initialization so far
  }

  @Override
  public void export(OutputStream out, SearchResult sr, User user) {
    ItemController ic = new ItemController();

    try {
      Collection<Item> itemList = ic.retrieveBatch(sr.getResults(), -1, 0, user);
      Items items = new Items(itemList);
      JaxbUtil.writeToOutputStream(items, out);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
