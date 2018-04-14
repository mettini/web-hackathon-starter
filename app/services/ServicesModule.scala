package services

import play.api.cache.ehcache.EhCacheComponents
import models.ModelsModule

trait ServicesModule extends ModelsModule 
  with EhCacheComponents {

  import com.softwaremill.macwire._
  import services.auth.{LoginService, SignupService, MemorySessionService}
  import services.user.{UserEmailService}
  import services.admin.{AdminUserService}

  lazy val appCacheApi = cacheApi("appcache")

  lazy val loginService = wire[LoginService]
  lazy val signupService = wire[SignupService]
  lazy val sessionService = wire[MemorySessionService]
  lazy val userEmailService = wire[UserEmailService]
  lazy val adminUserService = wire[AdminUserService]

}
