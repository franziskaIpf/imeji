package de.mpg.imeji.rest.resources.test.integration;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.rest.api.AlbumService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.resources.test.TestUtils;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.ItemTO;
import net.java.dev.webdav.jaxrs.ResponseStatus;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.JenaUtil;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;




@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlbumTest extends ImejiTestBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(AlbumTest.class);
	
	private static String pathPrefix = "/rest/albums";

	
	@Before
	public void specificSetup() {
		initAlbum();
		
	}

	@Test
	public void test_1_CreateAlbum_1_WithAuth() throws IOException {
		Path jsonPath = Paths
				.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity
						.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		assertEquals(response.getStatus(), CREATED.getStatusCode());
		Map<String, Object> albData = TestUtils.jsonToPOJO(response);
		assertNotNull("Created album is null", albData);
		albumId = (String) albData.get("id");
		assertThat("Empty album id", albumId,
				not(isEmptyOrNullString()));
	}
	
	@Test
	public void test_1_CreateAlbum_2_WithUnauth() throws IOException {
		Path jsonPath = Paths
				.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity
						.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		assertThat(response.getStatus(),equalTo(UNAUTHORIZED.getStatusCode()));
	}
	
	
	@Test
	public void test_2_ReadAlbum_1_WithAuth() throws ImejiException {
		Response response = target(pathPrefix).path(albumId)
				.register(authAsUser).request(MediaType.APPLICATION_JSON).get();

		String jsonString = response.readEntity(String.class);
		assertThat("Empty album", jsonString, not(isEmptyOrNullString()));
	}

	@Test
	public void test_2_ReadAlbum_2_WithUnauth() throws ImejiException {
		Response response = target(pathPrefix).path(albumId)
				.request(MediaType.APPLICATION_JSON).get();
		assertThat(response.getStatus(),equalTo(UNAUTHORIZED.getStatusCode()));

	}

	@Test
	public void test_2_ReadAlbum_3_WithNonAuth() throws ImejiException {
		Response response = target(pathPrefix).path(albumId)
				.register(authAsUser2).request(MediaType.APPLICATION_JSON)
				.get();
		assertThat(response.getStatus(), equalTo(FORBIDDEN.getStatusCode()));
	}

	@Test
	public void test_2_ReadAlbum_4_NonExistingAlbum() throws IOException {
		Response response = target(pathPrefix).path(albumId+"i_do_not_exist")
				.register(authAsUser).request(MediaType.APPLICATION_JSON)
				.get();
		assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
	}

	@Test
	public void test_2_ReadAlbum_5_ReadAlbumsWithQuery() throws IOException, UnprocessableError {
		Response response = target(pathPrefix)
				.queryParam("q", albumTO.getTitle())
				.register(authAsUser).request(MediaType.APPLICATION_JSON)
				.get();
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		List<AlbumTO> albumList = RestProcessUtils.buildTOListFromJSON(response.readEntity(String.class), AlbumTO.class);
		Assert.assertThat(albumList, not(empty()));
		Assert.assertThat(albumList.get(0).getTitle(), equalTo(albumTO.getTitle()));

	}

	@Test
	public void test_2_ReadAlbum_6_ReadAlbumItemsWithQuery() throws IOException, UnprocessableError {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		response = target(pathPrefix)
				.path("/" + albumId + "/members")
				.queryParam("q", itemTO.getFilename())
				.register(authAsUser).request(MediaType.APPLICATION_JSON)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		List<ItemTO> itemList = RestProcessUtils.buildTOListFromJSON(response.readEntity(String.class), ItemTO.class);
		Assert.assertThat(itemList, not(empty()));
		Assert.assertThat(itemList.get(0).getFilename(), equalTo(itemTO.getFilename()));

	}
	
	@Test
	public void test_2_ReadAlbum_6_ReadAlbumItemsWithQueryForNoResultsAndOneItemInAlbum() throws IOException, UnprocessableError {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		response = target(pathPrefix)
				.path("/" + albumId + "/members")
				.queryParam("q", itemTO.getFilename()+"_new")
				.register(authAsUser).request(MediaType.APPLICATION_JSON)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		List<ItemTO> itemList = RestProcessUtils.buildTOListFromJSON(response.readEntity(String.class), ItemTO.class);
		Assert.assertThat(itemList, empty());

	}
	
	@Test
	public void test_3_DeleteAlbum_1_WithAuth() throws ImejiException {

		initAlbum();

		Response response = target(pathPrefix)
				.path("/" + albumId).register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete();

		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
		
		response = target(pathPrefix).
				path(albumId).register(authAsUser).
				request(MediaType.APPLICATION_JSON)
				.get();
		
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
		
	}
	
	@Test
	public void test_3_DeleteAlbum_2_WithUnauth() throws ImejiException{
		initAlbum();

		Response response = target(pathPrefix)
				.path("/" + albumId).register(authAsUser2)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete();

		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}

	@Test
	public void test_3_DeleteAlbum_4_WithOutUser(){
		
		initAlbum();
		
		Response response = target(pathPrefix)
				.path("/" + albumId)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete();
		assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
		
	@Test
	public void test_3_DeleteAlbum_5_NonExistingAlbum(){
		Response response = target(pathPrefix)
				.path("/" + albumId + "i_do_not_exist").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete();

		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	@Test
	public void test_4_ReleaseAlbum_1_WithAuth() throws ImejiException {
		initCollection();
		initItem();		
		target(pathPrefix)
				.path("/" + albumId + "/release").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));
		
		target(pathPrefix)
		.path("/" + albumId + "/members/link").register(authAsUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("[\"" + itemId + "\"]"));
		
		Response response = target(pathPrefix)
		.path("/" + albumId + "/release").register(authAsUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("{}"));		

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		AlbumService s = new AlbumService();
		assertEquals("RELEASED", s.read(albumId, JenaUtil.testUser).getStatus());	
		
	}
	
	@Test
	public void test_4_ReleaseAlbum_2_WithUnauth() throws ImejiException{
		initCollection();
		initItem();
		target(pathPrefix)
		.path("/" + albumId + "/members/link").register(authAsUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("[\"" + itemId + "\"]"));
		
		Response response = target(pathPrefix)
		.path("/" + albumId + "/release").register(JenaUtil.testUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("{}"));		
		
		assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_4_ReleaseCollection_3_EmptyAlbum(){
		Response response = target(pathPrefix)
				.path("/" + albumId + "/release").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));	
		assertEquals(ResponseStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_4_ReleaseAlbum_4_WithUnauth(){
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/release")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));
		assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_4_ReleaseAlbum_5_ReleaseAlbumTwice() throws ImejiException{
		initCollection();
		initItem();		
		target(pathPrefix)
				.path("/" + collectionId + "/release").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));
		
		target(pathPrefix)
		.path("/" + albumId + "/members/link").register(authAsUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("[\"" + itemId + "\"]"));
		
		Response response = target(pathPrefix)
		.path("/" + albumId + "/release").register(authAsUser)
		.request(MediaType.APPLICATION_JSON_TYPE)
		.put(Entity.json("{}"));		

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		AlbumService s = new AlbumService();
		assertEquals("RELEASED", s.read(albumId, JenaUtil.testUser).getStatus());	
		
		response = target(pathPrefix)
				.path("/" + albumId + "/release").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));
		assertEquals(ResponseStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_4_ReleaseAlbum_6_NonExistingAlbum(){
		Response response = target(pathPrefix)
				.path("/" + albumId + "i_do_not_exist/release").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("{}"));	
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	
	
	@Test
	public void test_5_AddItemsToAlbum_1_WithAuth() throws ImejiException {
		initCollection();
		initItem();
		initAlbum();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_5_AddItemsToAlbum_2_WithUnauth() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));
	}
	
	@Test
	public void test_5_AddItemsToAlbum_3_WithNonAuth() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser2)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertThat(response.getStatus(), equalTo(FORBIDDEN.getStatusCode()));;
	}
	
	@Test
	public void test_5_AddItemsToAlbum_4_NonExistingAlbum() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId+"i_do_not_exist" + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	
	
	@Test
	public void test_5_AddItemsToAlbum_5_NonExistingItem() throws ImejiException {
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + "adfgsh" + "\"]"));	

		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	
	@Test
	public void test_6_WithdrawAlbum_1_WithAuth() throws ImejiException {
		
		initCollection();
		initItem();
	
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));
		
		AlbumService s = new AlbumService();
		s.release(albumId, JenaUtil.testUser);
		
		assertEquals("RELEASED", s.read(albumId, JenaUtil.testUser).getStatus());
		
		Form form= new Form();
		form.param("id", albumId);
		form.param("discardComment", "test_6_WithdrawAlbum_1_WithAuth_" + System.currentTimeMillis());
		response = target(pathPrefix)
				.path("/" + albumId + "/discard").register(authAsUser)
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(Status.OK.getStatusCode(), response.getStatus());	
		assertEquals("WITHDRAWN", s.read(albumId, JenaUtil.testUser).getStatus());
	}
	
	
	@Test
	public void test_6_WithdrawAlbum_2_WithUnauth() throws ImejiException{
		initCollection();
		initItem();
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		AlbumService s = new AlbumService();
		s.release(albumId, JenaUtil.testUser);
		
		assertEquals("RELEASED", s.read(albumId, JenaUtil.testUser).getStatus());
		
		Form form= new Form();
		form.param("id", albumId);
		form.param("discardComment", "test_6_WithdrawAlbum_2_WithUnAuth_" + System.currentTimeMillis());
		response = target(pathPrefix)
				.path("/" + albumId + "/discard").register(authAsUser2)
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	 }

	@Test
	public void test_6_WithdrawAlbum_3_WithNonAuth() throws ImejiException {
		initCollection();
		initItem();
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		AlbumService s = new AlbumService();
		s.release(albumId, JenaUtil.testUser);
		assertEquals("RELEASED", s.read(albumId, JenaUtil.testUser).getStatus());
		
		Form form= new Form();
		form.param("id", albumId);
		form.param("discardComment", "test_6_WithdrawAlbum_3_WithNonAuth_" + System.currentTimeMillis());
		response = target(pathPrefix)
				.path("/" + albumId + "/discard")
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	 }


	@Test
	public void test_6_WithdrawAlbum_4_NotReleasedAlbum() throws ImejiException {
		initCollection();
		initItem();
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		AlbumService s = new AlbumService();
		assertEquals("PENDING", s.read(albumId, JenaUtil.testUser).getStatus());
		
		Form form= new Form();
		form.param("id", albumId);
		form.param("discardComment", "test_6_WithdrawAlbum_4_NotReleasedCollection_" + System.currentTimeMillis());
		response = target(pathPrefix)
				.path("/" + albumId + "/discard").register(authAsUser)
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(ResponseStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_6_WithdrawAlbum_5_WithdrawAlbumTwice() throws ImejiException{
		initCollection();
		initItem();
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		AlbumService s = new AlbumService();
		s.release(albumId, JenaUtil.testUser);
		s.withdraw (albumId, JenaUtil.testUser,"test_6_WithdrawAlbum_5_WithdrawAlbumTwice_"+System.currentTimeMillis());

		assertEquals("WITHDRAWN", s.read(albumId, JenaUtil.testUser).getStatus());
		
		Form form= new Form();
		form.param("id", albumId);
		form.param("discardComment", "test_6_WithdrawAlbum_5_WithdrawAlbumTwice_SecondTime_"+System.currentTimeMillis());
		response = target(pathPrefix)
				.path("/" + albumId + "/discard").register(authAsUser)
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(ResponseStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_6_WithdrawAlbum_6_NotExistingAlbum() throws ImejiException{
		
		Form form= new Form();
		form.param("id", albumId + "i_do_not_exist");
		form.param("discardComment", "test_6_WithdrawAlbum_6_NotExistingAlbum_" + System.currentTimeMillis());
		Response response = target(pathPrefix)
				.path("/" + albumId + "i_do_not_exist/discard").register(authAsUser)
				.request((MediaType.APPLICATION_JSON_TYPE))
				.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_7_UpdateAlbum_1_WithAuth() throws IOException {
		Path jsonPath = Paths.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		jsonPath = Paths.get("src/test/resources/rest/updateAlbum.json");
		jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");
		
		jsonString = jsonString.replace("idValue", albumId);
		
		response = target(pathPrefix)
				.path("/" + albumId).register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Map<String, Object> albData = TestUtils.jsonToPOJO(response);
		assertEquals("TestAlbumUpdate", albData.get("title"));
	}
	
	@Test
	public void test_7_UpdateAlbum_2_WithUnAuth() throws IOException {
		Path jsonPath = Paths.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		jsonPath = Paths.get("src/test/resources/rest/updateAlbum.json");
		jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");
		
		jsonString = jsonString.replace("idValue", albumId);
		
		response = target(pathPrefix)
				.path("/" + albumId).register(authAsUser2)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_7_UpdateAlbum_3_WithNonAuth() throws IOException {
		Path jsonPath = Paths.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		jsonPath = Paths.get("src/test/resources/rest/updateAlbum.json");
		jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");
		
		jsonString = jsonString.replace("idValue", albumId);
		
		response = target(pathPrefix)
				.path("/" + albumId)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_7_UpdateAlbum_4_NonExistingAlbum() throws IOException {
		Path jsonPath = Paths.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		jsonPath = Paths.get("src/test/resources/rest/updateAlbum.json");
		jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");
		
		String nonExistingAlbumId = "testID";
		jsonString = jsonString.replace("idValue", nonExistingAlbumId);
		
		response = target(pathPrefix)
				.path("/" + nonExistingAlbumId).register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_7_UpdateAlbum_5_AlbumIdDiffersFromAlbumIdInJSON() throws IOException {
		Path jsonPath = Paths.get("src/test/resources/rest/createAlbum.json");
		String jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");

		Response response = target(pathPrefix)
				.register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		jsonPath = Paths.get("src/test/resources/rest/updateAlbum.json");
		jsonString = new String(Files.readAllBytes(jsonPath), "UTF-8");
		
		String nonExistingAlbumId = "testID";
		jsonString = jsonString.replace("idValue", nonExistingAlbumId);
		
		response = target(pathPrefix)
				.path("/" + albumId).register(authAsUser)
				.register(MultiPartFeature.class)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}
	
	
	@Test
	public void test_8_UnlinkItemsToAlbum_1_WithAuth() throws ImejiException {
		initCollection();
		initItem();
		initAlbum();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/unlink").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_8_UnlinkItemsToAlbum_2_WithUnauth() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/unlink")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));
	}
	
	@Test
	public void test_8_UnlinkItemsToAlbum_3_WithNonAuth() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/unlink").register(authAsUser2)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertThat(response.getStatus(), equalTo(FORBIDDEN.getStatusCode()));;
	}
	
	@Test
	public void test_8_UnlinkItemsToAlbum_4_NonExistingAlbum() throws ImejiException {
		initCollection();
		initItem();
		
		Response response = target(pathPrefix)
				.path("/" + albumId+"i_do_not_exist" + "/members/unlink").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	

		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	
	
	@Test
	public void test_8_UnlinkItemsToAlbum_5_NonExistingItem() throws ImejiException {
		//in principle it does not care if item exists or not, it will simply do nothing
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/unlink").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + "adfgsh" + "\"]"));	

		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void test_8_UnlinkAllItemsFromAlbum() throws ImejiException {
		//in principle it does not care if item exists or not, it will simply do nothing
		initCollection();
		initItem("test");
		
		Response response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		initItem("test2");

		response = target(pathPrefix)
				.path("/" + albumId + "/members/link").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(Entity.json("[\"" + itemId + "\"]"));	
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		//read two items from the Album
		response = target(pathPrefix)
				.path("/" + albumId + "/members")
				.register(authAsUser).request(MediaType.APPLICATION_JSON)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		List<ItemTO> itemList = RestProcessUtils.buildTOListFromJSON(response.readEntity(String.class), ItemTO.class);
		Assert.assertThat(itemList, not(empty()));
		Assert.assertEquals(2, itemList.size());

		//remove all album mebres
		response = target(pathPrefix)
				.path("/" + albumId + "/members").register(authAsUser)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.delete();	
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
		
		//read no items from the Album
		response = target(pathPrefix)
						.path("/" + albumId + "/members")
						.register(authAsUser).request(MediaType.APPLICATION_JSON)
						.get();
				
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		itemList = RestProcessUtils.buildTOListFromJSON(response.readEntity(String.class), ItemTO.class);
		Assert.assertThat(itemList, empty());
	}
	

}
