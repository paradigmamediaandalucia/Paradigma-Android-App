
# <img src="img/LogoP.png" alt="Logo Paradigma Media" width="48" /> Paradigma Media  

![Logo Paradigma Media](img/LogoRadio.png)

## Descripción del Proyecto

Paradigma App es una aplicación móvil desarrollada con Kotlin Multiplatform (KMP) que ofrece a los usuarios una experiencia auditiva completa del contenido de Paradigma Media. Permite escuchar programas de radio, episodios de podcasts bajo demanda y la transmisión en vivo de Andaina FM.

La arquitectura del proyecto se centra en una lógica de negocio compartida en Kotlin, asegurando consistencia y eficiencia entre las plataformas Android (Jetpack Compose) e iOS (SwiftUI). La app implementa un enfoque offline-first gracias a una caché local con SQLDelight, permitiendo una navegación fluida y acceso al contenido sin conexión, de forma limitada, el audio es tmabién offline si se ha descargado previamente.

Para una explicación detallada de la justificación, los objetivos y la arquitectura, consulta la **[Documentación del Proyecto en la Wiki](https://github.com/MarioAJ11/Paradigma-App/wiki/Doc_PI)**.

## Información sobre Despliegue

El despliegue de la aplicación requiere la compilación de los módulos nativos (`androidApp`, `iosApp`) y la configuración de sus dependencias y firmas de código.

* **Android**: Es imprescindible generar un Keystore y configurar el archivo `androidApp/build.gradle.kts` para firmar el App Bundle (`.aab`) de lanzamiento, lo cual ya esta hehco y en proceso de subida.
* **iOS**: Se debe configurar un equipo de desarrollo y un provisioning profile en Xcode.

Para ver los pasos detallados, consulta el **[Manual de Despliegue en la Wiki](https://github.com/MarioAJ11/Paradigma-App/wiki/Manual_Despliegue)**.

## Información sobre cómo usarlo

Paradigma App está diseñada para ser intuitiva. Gracias a la caché local, la navegación por programas y episodios es casi instantánea después de la primera carga.

1.  **Explora**: Usa la barra de navegación inferior para descubrir contenido: Inicio, Buscar, Continuar, Descargas, Cola y Ajustes.
2.  **Reproduce**: Toca cualquier episodio para iniciar la reproducción. Un reproductor compacto aparecerá en la parte inferior.
3.  **Escucha Offline**: Descarga episodios para escucharlos sin conexión a internet desde la sección "Descargas".

Para una guía completa con todas las funcionalidades, consulta el **[Manual de Usuario en la Wiki](https://github.com/MarioAJ11/Paradigma-App/wiki/Manual_Usuario)**.

## Autor
<h2>
    <img alt="Logo de GitHub" src="img/github-mark-white.png" width="30" style="vertical-align:left;"> 
    <a href="https://github.com/MarioAJ11">MarioAJ11</a><br>
    <img src="img/LogoP.png" alt="Logo Paradigma Media" width="30"/>
    <a href="https://paradigmamedia.org/?_gl=1%2Aae219k%2A_ga%2AODk2ODAwOTI0LjE3NDMxMjI3MTA.%2A_ga_XR0VQ8N0YB%2AczE3NDc5NTIwNDMkbzQ2JGcxJHQxNzQ3OTUzNDI3JGowJGwwJGgw"> Paradigma Media Andalucía</a>
</h2>
