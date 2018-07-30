El objetivo de este ejercicio es llevar a cabo las refactorizaciones las clases situadas en el paquete `com.mycorp`, que es código legacy de una aplicación, para que pueda aspirar a tener una cobertura de al menos el 80%.

El tiempo para hacer el ejercicio es de hasta cuatro horas a partir de la recepción del correo con este ejercicio, plazo máximo en el que debe referirse una URL de github (o similares) a la dirección de arquitecturainternet@sanitas.es para su análisis. Es imposible, dentro del tiempo previsto para el ejercicio, hacer todas la refactorizaciones que deberían llevarse a cabo, por lo que el objeto del ejercicio NO es dejar la clase bien, sino ver cómo se plantea la solución.


Notas adicionales
=================

* El uso habitual de este clase es como un bean que se instancia a través del contexto de Spring.

* Se incluyen unos cuantos jars que se corresponden con diversos stubs de WS ya existentes, que deben instalarse en el repo local de Maven para poder realizar la compilzación inicial. Las coordenadas que tienen que llevar dichos artefactos se encuentran comentadas en el fichero pom.xml.

* Es básicamente imposible dejar la clase en condiciones en el tiempo propuesto; más que dejarla perfecta, lo que se busca ver es la manera en que se afronta el problema. En este sentido, no hay una solución correcta, es mucho más interesante el histórico de Git como hilo conductor de lo que se va haciendo. Tener un único commit con todas las actuaciones efectuadas no es aconsejable.

* En el paquete com.mycorp.soporte hay un montón de clases necesarias para la compilación de las clases del paquete `com.mycorp`. NO es necesario tocarlas, no forma parte del ejercicio, están ahí para poder compilar.

* El entregable debe compilar, en una instalación de maven totalmente limpia, debe indicarse en un fichero README los pasos necesarios para generar el jar final (no hace falta mucho detalle).

* Hay libertad total para hacer cualquier cosa que mejore el código (añadir tantas nuevas librerías como se crea necesario, actualizar plugins, configuraciones, etc.).

* Si se desea ver un informe de cobertura del módulo en cualquier momento, se puede obtener ejecutando el comando `$MAVEN_HOME/bin/mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report`. El informe queda generado en la subcarpeta `./target/site/jacoco`.

* Reservar los últimos 15 minutos para indicar por correo acciones adicionales que podrían seguir realizándose.