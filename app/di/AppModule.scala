package di

import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.ServicesModule
import play.api.i18n.{I18nComponents, MessagesApi}

trait AppModule extends ServicesModule
  with I18nComponents {

  import com.softwaremill.macwire._
  import controllers.PingController
  import controllers.user.{ChangePasswordController, EmailVerificationController, UserProfileController}
  import controllers.auth.{LoginController, LogoutController, SignupController, RecoverPasswordController}
  import controllers.www.{HomeController}
  import controllers.admin.{AdminHomeController}
  import controllers.admin.auth.{AdminLoginController, AdminLogoutController}
  import controllers.admin.adminUser.{AdminUserController}
  import controllers.admin.user.{UserController}
  import common.ui.ViewBuilder

  // common
  lazy val viewBuilder = wire[ViewBuilder]

  // gral
  lazy val pingController = wire[PingController]

  // auth
  lazy val loginController = wire[LoginController]
  lazy val logoutController = wire[LogoutController]
  lazy val signupController = wire[SignupController]
  lazy val recoverPasswordController = wire[RecoverPasswordController]
  
  // user
  lazy val changePasswordController = wire[ChangePasswordController]
  lazy val emailVerificationController = wire[EmailVerificationController]
  lazy val userProfileController = wire[UserProfileController]

  // www
  lazy val homeController = wire[HomeController]

  // admin
  lazy val adminLoginController = wire[AdminLoginController]
  lazy val adminLogoutController = wire[AdminLogoutController]
  lazy val adminHomeController = wire[AdminHomeController]
  lazy val adminUserController = wire[AdminUserController]
  lazy val userController = wire[UserController]

  def langs: Langs
  def controllerComponents: ControllerComponents

}
