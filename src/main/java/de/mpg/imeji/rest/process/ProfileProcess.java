package de.mpg.imeji.rest.process;

import static javax.ws.rs.core.Response.Status.OK;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.ProfileService;
import de.mpg.imeji.rest.to.JSONResponse;

public class ProfileProcess {

  public static JSONResponse readProfile(HttpServletRequest req, String id) {
    JSONResponse resp;

    ProfileService pcrud = new ProfileService();
    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(Status.OK.getStatusCode(), pcrud.read(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());

    }
    return resp;
  }

  public static JSONResponse deleteProfile(HttpServletRequest req, String id) {
    JSONResponse resp;

      ProfileService pcrud = new ProfileService();
      try {
        User u = BasicAuthentication.auth(req);
        resp = RestProcessUtils.buildResponse(Status.OK.getStatusCode(), pcrud.delete(id, u));
      } catch (Exception e) {

        resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
      }
    return resp;

  }

  public static JSONResponse readAll(HttpServletRequest req, String q) {
    JSONResponse resp;


    ProfileService ccrud = new ProfileService();
    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), ccrud.readAll(u, q));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse releaseProfile(HttpServletRequest req, String id) {
    JSONResponse resp;
    ProfileService service = new ProfileService();

    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), service.release(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse withdrawProfile(HttpServletRequest req, String id,
      String discardComment) {
    JSONResponse resp;
    ProfileService service = new ProfileService();
    try {
      User u = BasicAuthentication.auth(req);
      resp =
          RestProcessUtils.buildResponse(OK.getStatusCode(),
              service.withdraw(id, u, discardComment));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }
  
  /**
   * Returns an item template of a {@link CollectionImeji}
   * 
   * @param req
   * @param id
   * @return
   */
  public static JSONResponse readItemTemplate(HttpServletRequest req, String id) {
    JSONResponse resp = null;

    ProfileService pcrud = new ProfileService();
    try {
          User u = BasicAuthentication.auth(req);

          resp =
              RestProcessUtils.buildResponse(Status.OK.getStatusCode(),
                  pcrud.readItemTemplate(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }
}
