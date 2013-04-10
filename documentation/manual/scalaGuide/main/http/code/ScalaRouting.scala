package scalaguide.http.routing

import org.specs2.mutable.Specification
import play.api.test.FakeRequest
import play.api.mvc._
import play.api.test.Helpers._
import play.core.Router

package controllers {

  object Client {
    def findById(id: Long) = Some("showing client " + id)
  }

  object Clients extends Controller {

    // #show-client-action
    def show(id: Long) = Action {
      Client.findById(id).map { client =>
        Ok(views.html.Clients.display(client))
      }.getOrElse(NotFound)
    }
    // #show-client-action

    def list() = Action(Ok("all clients"))
  }

  object Application extends Controller {
    def download(name: String) = Action(Ok("download " + name))
    def homePage() = Action(Ok("home page"))

    def loadContentFromDatabase(page: String) = Some("showing page " + page)

    // #show-page-action
    def show(page: String) = Action {
      loadContentFromDatabase(page).map { htmlContent =>
        Ok(htmlContent).as("text/html")
      }.getOrElse(NotFound)
    }
    // #show-page-action
  }

  object Items extends Controller {
    def show(id: Long) = Action(Ok("showing item " + id))
  }

  object Api extends Controller {
    def list(version: Option[String]) = Action(Ok("version " + version))
  }
}

package query {
  package object controllers {
    val Application = scalaguide.http.routing.controllers.Application
  }
}

package fixed {
  package object controllers {
    val Application = scalaguide.http.routing.controllers.Application
  }
}

package defaultvalue.controllers {
  object Clients extends Controller {
    def list(page: Int) = Action(Ok("clients page " + page))
  }
}

object ScalaRoutingSpec extends Specification {
  "the scala router" should {
    "support simple routing with a long parameter" in {
      contentOf(FakeRequest("GET", "/clients/10")).trim must_== "showing client 10"
    }
    "support a static path" in {
      contentOf(FakeRequest("GET", "/clients/all")) must_== "all clients"
    }
    "support a path part that spans multiple segments" in {
      contentOf(FakeRequest("GET", "/files/foo/bar")) must_== "download foo/bar"
    }
    "support regex path parts" in {
      contentOf(FakeRequest("GET", "/items/20")) must_== "showing item 20"
    }
    "support parameterless actions" in {
      contentOf(FakeRequest("GET", "/")) must_== "home page"
    }
    "support passing parameters from the path" in {
      contentOf(FakeRequest("GET", "/foo")) must_== "showing page foo"
    }
    "support passing parameters from the query string" in {
      contentOf(FakeRequest("GET", "/?page=foo"), query.Routes) must_== "showing page foo"
    }
    "support fixed values for parameters" in {
      contentOf(FakeRequest("GET", "/foo"), fixed.Routes) must_== "showing page foo"
      contentOf(FakeRequest("GET", "/"), fixed.Routes) must_== "showing page home"
    }
    "support default values for parameters" in {
      contentOf(FakeRequest("GET", "/clients"), defaultvalue.Routes) must_== "clients page 1"
      contentOf(FakeRequest("GET", "/clients?page=2"), defaultvalue.Routes) must_== "clients page 2"
    }
    "support optional values for parameters" in {
      contentOf(FakeRequest("GET", "/api/list-all")) must_== "version None"
      contentOf(FakeRequest("GET", "/api/list-all?version=3.0")) must_== "version Some(3.0)"
    }

  }

  def contentOf(rh: RequestHeader, router: Router.Routes = Routes) = contentAsString(router.routes(rh) match {
    case e: EssentialAction => AsyncResult(e(rh).run)
  })
}
