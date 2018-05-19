# Error
error.emptyfirstName=Debes ingresar un Nombre
error.emptylastName=Debes ingresar un Apellido
error.emptyEmail=Debes ingresar un Email
error.emptyUsername=Debes ingresar un Nombre de usuario
error.invalidEmail=El email ingresado es incorrecto
error.emptyPassword=Debes ingresar una contraseña
error.emptyRepeatPassword=Debes repetir la contraseña
error.passwordTooShort=La contraseña debe contener por lo menos 8 caracteres
error.passwordsNotMatch=Las contraseñas no coinciden
error.admin.passwordTooShort=La contraseña debe contener por lo menos 12 caracteres

error.emptyCurrentPassword=Debes ingresar la contraseña actual
error.currentPasswordTooShort=La contraseña actual debe contener por lo menos 8 caracteres
error.emptyNewPassword=Debes ingresar una nueva contraseña
error.newPasswordTooShort=La nueva contraseña debe contener por lo menos 8 caracteres
error.selectARole=Debes seleccionar un role

error.auth.cannotSingup=No se ha podido realizar el registro. Intenta nuevamente
error.auth.cannotLogin=Email y/o contraseña inválidos
error.auth.cannotRecover=No se puede recuperar la contraseña. Intenta nuevamente
error.admin.cannotLogin=Nombre de usuario y/o contraseña inválidos
error.email.notFound=El email ingresado no corresponde con un usuario registrado
error.username.notFount=El nombre de usuario ingresado no corresponde con un administrador registrado
error.user.notFound=Los datos ingresados no corresponde con un usuario registrado
error.user.cannotEditProfile=No se pudo actualizar el perfil. Intenta nuevamente
error.user.cannotModifyPassword=No se pudo actualizar la contraseña. Intenta nuevamente
error.user.cannotDelete=No se pudo borrar el usuario. Intenta nuevamente
error.user.cannotModerate=No se pudo moderar el usuario. Intenta nuevamente
error.user.cannotRemoveModeration=No se pudo remover la moderación del usuario. Intenta nuevamente
error.admin.auth.cannotCreate=No se ha podido crear el usuario administrador. Intenta nuevamente
error.admin.auth.cannotEdit=No se ha podido editar el usuario administrador. Intenta nuevamente
error.adminUser.notExist=El ID de usuario administrador provisto es inválido
error.adminUser.cannotDelete=No se pudo borrar el usuario administrador. Intenta nuevamente
error.adminUser.cannotModifyPassword=No se pudo actualizar la contraseña. Intenta nuevamente
error.superAdmin.edit=No cuentas con los permisos suficientes para editar el Super Admin

# Actions
actions.logout=Salir
actions.signup=Registrarse
actions.login=Ingresar
actions.recover=Restablecer contraseña
actions.back=Volver
actions.resend=Reenviar
actions.resendEmailVerification=Reenviar email validación
actions.modify=Modificar
actions.adminUsers=Usuarios administradores
actions.users=Usuarios
actions.edit=Editar
actions.delete=Borrar
actions.detail=Detalle
actions.create=Crear
actions.editPassword=Editar contraseña
actions.moderate=Moderar
actions.removeModeration=Remover moderación

# Fields
field.name=Nombre
field.firstName=Nombre
field.lastName=Apellido
field.email=Email
field.password=Contraseña
field.repeatPassword=Repetir contraseña
field.currentPassword=Contraseña actual
field.newPassword=Nueva contraseña
field.username=Nombre de usuario
field.id=ID
field.actions=Acciones
field.roles=Roles
field.verificationStatus=Estado verificación
field.registrationDate=Fecha registro
field.moderated=Moderado
field.testUser=Usuario test

# Paginator
paginator.previous=Anterior
paginator.next=Siguiente

# Email Verification
email.verification.verified=Email verificado
email.verification.pending=Verificación pendiente
email.verification.expired=Verificación expirada

# Signup
signup.title=Registro
signup.createAccount=Crear Cuenta

# Login
login.title=Ingreso
login.loginAction=Ingresar

# Home
home.title=Hackathon Starter

# Logout
logout.error=Problemas al salir de la aplicación

# Recover
recover.title=Restablecer contraseña
recover.recoverAction=Restablecer
recover.success=Recuperación iniciada. Te hemos enviado un correo electrónico con los pasos a seguir

# Activation
emailVerification.result.title=Validación correo electrónico
emailVerification.verified=La dirección {0} fue correctamente verificada.
emailVerification.error.alreadyVerified=La dirección ya se encuentra verificada.
emailVerification.error.hashNotFound=El link que ingresaste es incorrecto. Verifica el correo que te hemos enviado.
emailVerification.error.hashExpired=El correo de verificación ha expirado. Debes solicitar un nuevo correo de verificación.
emailVerification.error.resend=No se ha podido reenviar el correo de verificación. Intenta nuevamente
emailVerification.sent=Se ha enviado exitósamente el correo de verificación a {0}

# Reset Password
resetPassword.success=La contraseña fue restablecida.
emailResetPassword.error.hashExpired=El correo de restablecimiento de contraseña ha expirado. Debes solicitar el restablecimiento nuevamente.
emailResetPassword.error.alreadyVerified=La contraseña ya fue reestablecida. Si deseas restablecerla, solicita el restablecimiento.
emailResetPassword.error.hashNotFound=El link que ingresaste incorrecto. Verifica el correo que te hemos enviado.

# User Profile
userProfile.title=Perfil
userProfile.modifyPassword=Cambiar contraseña
userProfile.edit=Editar perfil

# Edit User Profile
editUserProfile.title=Editar perfil
editUserProfile.success=El perfil se actualizó exitosamente.
editUserProfile.successButEmailValidation=El perfil se actualizó exitosamente. Te hemos enviado un correo de verificación a {0}
editUserProfile.edit=Editar perfil

# Change Password
changePassword.title=Modificar contraseña
changePassword.success=Se modificó exitósamente la contraseña
changePassword.invalidCurrentPassword=La contraseña actual es incorrecta

# Admin
admin.login.title=Ingreso Administradores
admin.home.title=Panel Administradores
admin.logout.error=Prolemas al salir del panel de administración

# Admin User
adminUser.create.title=Crear usuario administrador
adminUser.create.createUser=Crear usuario
adminUser.created.successfully=Usuario creado con éxito
adminUser.edit.title=Editar usuario administrador
adminUser.edit.successfully=Usuario editado con éxito
adminUser.editPassword.successfully=Contraseña actualizada con éxito
adminUser.edit.editUser=Editar usuario
adminUser.list.title=Usuarios administradores
adminUser.delete.text=¿Estás segudo que quieres borrar el usuario admnistrador #{0}? El cambio es irreversible.
adminUser.delete.successfully=Usuario borrado con éxito
adminUser.editPassword.title=Editar contraseña

# User (Admin panel)
user.list.title=Usuarios
user.delete.text=¿Estás segudo que quieres borrar el usuario #{0}? El cambio es irreversible.
user.delete.successfully=Usuario borrado con éxito
user.detail.title=Detalle de usuario
user.detail.basicInformation=Información básica
user.detail.config=Configuración
user.detail.profile=Perfil
