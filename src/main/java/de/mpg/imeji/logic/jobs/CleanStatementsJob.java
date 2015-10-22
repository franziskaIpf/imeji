package de.mpg.imeji.logic.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.query.SPARQLQueries;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.writer.WriterFacade;
import de.mpg.j2j.annotations.j2jId;

/**
 * Clean empty {@link MetadataProfile}, which are not referenced by any collection
 * 
 * @author saquet
 *
 */
public class CleanStatementsJob implements Callable<Integer> {
  private static final Logger logger = Logger.getLogger(CleanStatementsJob.class);

  @Override
  public Integer call() throws Exception {
    logger.info("Cleaning statements...");
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchSimpleForQuery(SPARQLQueries.selectStatementUnbounded()).getResults();
    removeResources(uris, Imeji.profileModel, new Statement());
    logger.info("...done!");
    return 1;
  }

  /**
   * Remove Exception a {@link List} of {@link Resource}
   * 
   * @param uris
   * @param modelName
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws Exception
   */
  private void removeResources(List<String> uris, String modelName, Object obj)
      throws InstantiationException, IllegalAccessException, Exception {
    removeObjects(loadResourcesAsObjects(uris, modelName, obj), modelName);
  }

  /**
   * Remove an {@link Object}, it must have a {@link j2jId}
   * 
   * @param l
   * @param modelName
   * @throws Exception
   */
  private void removeObjects(List<Object> l, String modelName) throws Exception {
    WriterFacade writer = new WriterFacade(modelName);
    writer.delete(l, Imeji.adminUser);
  }

  /**
   * Load the {@link Resource} as {@link Object}
   * 
   * @param uris
   * @param modelName
   * @param obj
   * @return
   */
  private List<Object> loadResourcesAsObjects(List<String> uris, String modelName, Object obj) {
    ReaderFacade reader = new ReaderFacade(modelName);
    List<Object> l = new ArrayList<Object>();
    for (String uri : uris) {
      try {
        l.add(reader.read(uri, Imeji.adminUser, obj.getClass().newInstance()));
      } catch (Exception e) {
        logger.error("ERROR LOADING RESOURCE " + uri + " !!!!!", e);
      }
    }
    return l;
  }
}