# Web Hackathon Starter

It is a hackathon starter made in Scala (Play Framework). The idea is to have a base project in which to start a hackathon or use it as a boilerplate to start new a project.

It covers the base structure of the project, user management and a mini admin panel where registered users can be seen.

It aims to be a web application. In the case that the nature of the project is only API-based, take a look to [API Hackathon Starter](https://github.com/mettini/api-hackathon-starter).

Table of Contents
-----------------

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Obtaining API Keys](#obtaining-api-keys)
- [Contributing](#contributing)
- [License](#license)

Features
--------

- Authentication using email and password
- MVC project structure
- Bootstrap 4
- JQuery 3
- SASS support
- CSRF protection
- I18n (en & es)
- Email send through Sendgrid (dev mode logging emails in output log)
- **Account management**
    - Edit profile
    - Reset password
    - Email verification
    - Change password
- **Admin panel**
    - CRUD admin users
    - Unique Super Admin
    - Role scheme for admin users
    - Registered users list
    - Registered user profile
    - User moderation
    - User delete
    - Test users / Real users discrimination

Prerequisites
-------------
- [Mysql](http://www.mysql.com)
- [Java](https://www.java.com/es/download/)
- [SBT](https://www.scala-sbt.org/)

Getting Started
---------------

Clone the repo and follow these steps to leave the app running:

### Database init

Create a database with name *hackathonStarter*:
```sql
DROP DATABASE IF EXISTS hackathonStarter;
CREATE DATABASE hackathonStarter DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE hackathonStarter;
```

Create table structure and insert base data running `conf/evolutions/default/1.sql` and `conf/evolutions/default/2.sql` (only the *!Ups* sections in both files).

### Server startup

Open a console, and run `sbt` command. Once the sbt is running execute `run`.
When the startup has finished (first time it take a while to download all dependencies), open a browser and go to `http://localhost:9000` for web, and `http://localhost:9000/admin` for admin panel.

To access the panel please login with `admin` username and the `&rX3uZAwGE7JyuBW]h?{JX` password. It's strongly recommended that you change the password the first time you get into the admin panel.

Obtaining API Keys
------------------

By default the email sent is turn off (all emails will be print in the output log). To activate it follow the next steps:

- Create an account at [Sendgrid](https://sendgrid.com/)
- Obtain an api key and change the default value at `conf/application.conf` in `app.sendgrid.apikey` param. You may want to change the `app.sendgrid.from` param too.
- Turn on email send by changing to `true` the `email.enabled` param.

## Contributing

Thank you for considering contributing to Web Hackathon Starter.

## License

The MIT License (MIT). Please see [License File](LICENSE) for more information.