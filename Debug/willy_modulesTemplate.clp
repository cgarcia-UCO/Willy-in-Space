(defrule MAIN::passToMyMAIN
	=>
	(focus myMAIN))


;==========================================
(defmodule myMAIN (import MAIN deftemplate ?ALL) (import InternalFunctions deffunction ?ALL) (export deftemplate ?ALL))

; Se sugiere que éste sea vuestro módulo principal, que coordina las acciones generales de vuestro agente

; Se puede crear cualquier constructor que deseeis y que lo acepte el programa (funciones, plantillas, reglas...)




;=====================================
; Se pueden crear otros módulos (siempre que lo acepte el programa) con el contenido que se desee
; Se deben especificar las indicaciones de importación y exportación que deseeis, pero se sugiere importar todo de MAIN y de vuestro myMAIN
; Ejemplo:
(defmodule Modulo1 (import MAIN deftemplate ?ALL) (import myMAIN deftemplate ?ALL))

; Se puede crear cualquier constructor que deseis y que lo acepte el programa (funciones, plantillas, reglas...)





;=========================================
; Otros módulos (tantos como se deseen)
