package com.mycorp;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.DatosCliente;
import com.mycorp.support.MensajeriaService;
import com.mycorp.support.Poliza;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;
import com.mycorp.support.Ticket;
import com.mycorp.support.ValueCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

@Service
@PropertySource("classpath:t2.properties")
public class ZendeskService {


    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger( ZendeskService.class );

    private static final String ESCAPED_LINE_SEPARATOR;
    private static final String ESCAPE_ER;
    private static final String INI = "Inicio: ";
    private static final String FIN = "Fin: ";

    static {
        ESCAPED_LINE_SEPARATOR = "\\n";
        ESCAPE_ER = "\\";
    }

    private static final String HTML_BR = "<br/>";

    @Value("${zendesk.ticket}")
    private String PETICION_ZENDESK;

    @Value("${zendesk.token}")
    private String TOKEN_ZENDESK;

    @Value("${zendesk.url}")
    private String URL_ZENDESK;

    @Value("${zendesk.user}")
    private String ZENDESK_USER;

    @Value("${tarjetas.getDatos}")
    private String TARJETAS_GETDATOS ;

    @Value("${cliente.getDatos}")
    private String CLIENTE_GETDATOS ;

    @Value("${zendesk.error.mail.funcionalidad}")
    private String ZENDESK_ERROR_MAIL_FUNCIONALIDAD ;

    @Value("${zendesk.error.destinatario}")
    private String ZENDESK_ERROR_DESTINATARIO ;

    private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");


    /** The portalclientes web ejb remote. */
    @Autowired
    // @Qualifier("portalclientesWebEJB")
    private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier( "emailService" )
    private MensajeriaService emailService;

    /**
     * Crea un ticket en Zendesk. Si se ha informado el nÂº de tarjeta, obtiene los datos asociados a dicha tarjeta de un servicio externo.
     * @param usuarioAlta
     * @param userAgent
     */
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent){
        LOG.info(INI +getMethodName());
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String idCliente = null;
        StringBuilder datosUsuario = new StringBuilder();
        StringBuilder datosBravo = new StringBuilder();
        StringBuilder clientName = new StringBuilder();
        StringBuilder datosServicio = new StringBuilder();

        addDatosFormulario(usuarioAlta, userAgent, datosUsuario);

        datosBravo.append(ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:" + ESCAPED_LINE_SEPARATOR + ESCAPED_LINE_SEPARATOR);

        idCliente = getIdCliente(usuarioAlta, mapper, idCliente, clientName, datosServicio);
        getDatosCliente(datosBravo, idCliente);

        String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(), datosUsuario.toString()+datosBravo.toString()+
                parseJsonBravo(datosServicio));
        ticket = ticket.replaceAll("["+ESCAPED_LINE_SEPARATOR+"]", " ");

        crearTicket(mapper, datosUsuario, datosBravo, ticket);

        datosUsuario.append(datosBravo);
        LOG.info(FIN +getMethodName());
        return datosUsuario.toString();
    }

    private String getIdCliente(UsuarioAlta usuarioAlta,ObjectMapper mapper,String idCliente,
        StringBuilder clientName,StringBuilder datosServicio) {
        LOG.info(INI +getMethodName());
        if(StringUtils.isNotBlank(usuarioAlta.getNumTarjeta())){
            idCliente = altaNumtarjeta(usuarioAlta, mapper, idCliente, clientName, datosServicio);
        }else if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
            idCliente = altaNumPoliza(usuarioAlta, mapper, idCliente, clientName, datosServicio);
        }
        return idCliente;
    }

    private void addDatosFormulario(UsuarioAlta usuarioAlta, String userAgent, StringBuilder datosUsuario) {
        LOG.info(INI +getMethodName());
        if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
            datosUsuario.append("NÂº de poliza/colectivo: ").append(usuarioAlta.getNumPoliza()).append("/").append(usuarioAlta.getNumDocAcreditativo()).append(ESCAPED_LINE_SEPARATOR);
        }else{
            datosUsuario.append("NÂº tarjeta Sanitas o Identificador: ").append(usuarioAlta.getNumTarjeta()).append(ESCAPED_LINE_SEPARATOR);
        }
        datosUsuario.append("Tipo documento: ").append(usuarioAlta.getTipoDocAcreditativo()).append(ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("NÂº documento: ").append(usuarioAlta.getNumDocAcreditativo()).append(ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("Email personal: ").append(usuarioAlta.getEmail()).append(ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("NÂº mÃ³vil: ").append(usuarioAlta.getNumeroTelefono()).append(ESCAPED_LINE_SEPARATOR);
        datosUsuario.append("User Agent: ").append(userAgent).append(ESCAPED_LINE_SEPARATOR);
    }

    private static String getMethodName() {
        if (Thread.currentThread().getStackTrace().length>2) {
            return Thread.currentThread().getStackTrace()[2].getMethodName();
        } else {
            return "undefined";
        }
    }
    private void crearTicket(ObjectMapper mapper, StringBuilder datosUsuario, StringBuilder datosBravo, String ticket) {
        LOG.info(INI +getMethodName());
        try(Zendesk zendesk = new Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK).build()){
            //Ticket
            Ticket petiZendesk = mapper.readValue(ticket, Ticket.class);
            zendesk.createTicket(petiZendesk);

        }catch(Exception e){
            LOG.error("Error al crear ticket ZENDESK", e);
            sendEmail(datosUsuario, datosBravo);

        }
        LOG.info(FIN+getMethodName());
    }

    private void sendEmail(StringBuilder datosUsuario, StringBuilder datosBravo) {
        LOG.info(INI +getMethodName());
        CorreoElectronico correo = new CorreoElectronico( Long.parseLong(ZENDESK_ERROR_MAIL_FUNCIONALIDAD), "es" )
                .addParam(datosUsuario.toString().replaceAll(ESCAPE_ER+ESCAPED_LINE_SEPARATOR, HTML_BR))
                .addParam(datosBravo.toString().replaceAll(ESCAPE_ER+ESCAPED_LINE_SEPARATOR, HTML_BR));
        correo.setEmailA( ZENDESK_ERROR_DESTINATARIO );
        try
        {
            emailService.enviar( correo );
        }catch(Exception ex){
            LOG.error("Error al enviar mail", ex);
        }
    }

    private void getDatosCliente(StringBuilder datosBravo, String idCliente) {
        LOG.info(INI +getMethodName());
        try
        {
            // Obtenemos los datos del cliente
            DatosCliente cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosCliente.class, idCliente);

            datosBravo.append("TelÃ©fono: ").append(cliente.getGenTGrupoTmk()).append(ESCAPED_LINE_SEPARATOR);


            datosBravo.append("Feha de nacimiento: ").append(formatter.format(formatter.parse(cliente.getFechaNacimiento()))).append(ESCAPED_LINE_SEPARATOR);

            List< ValueCode > tiposDocumentos = getTiposDocumentosRegistro();
            for (ValueCode valueCode:tiposDocumentos) {
                if(valueCode.getCode().equals(cliente.getGenCTipoDocumento().toString()))
                {
                    datosBravo.append("Tipo de documento: ").append(valueCode.getValue()).append(ESCAPED_LINE_SEPARATOR);
                }
            }

            datosBravo.append("NÃºmero documento: ").append(cliente.getNumeroDocAcred()).append(ESCAPED_LINE_SEPARATOR);

            datosBravo.append("Tipo cliente: ");
            switch (cliente.getGenTTipoCliente()) {
            case 1:
                datosBravo.append("POTENCIAL").append(ESCAPED_LINE_SEPARATOR);
                break;
            case 2:
                datosBravo.append("REAL").append(ESCAPED_LINE_SEPARATOR);
                break;
            case 3:
                datosBravo.append("PROSPECTO").append(ESCAPED_LINE_SEPARATOR);
                break;
            }

            datosBravo.append("ID estado del cliente: ").append(cliente.getGenTStatus()).append(ESCAPED_LINE_SEPARATOR);

            datosBravo.append("ID motivo de alta cliente: ").append(cliente.getIdMotivoAlta()).append(ESCAPED_LINE_SEPARATOR);

            datosBravo.append("Registrado: ").append((cliente.getfInactivoWeb() == null ? "Sí" : "No")).append(ESCAPED_LINE_SEPARATOR + ESCAPED_LINE_SEPARATOR);


        }catch(Exception e)
        {
            LOG.error("Error al obtener los datos en BRAVO del cliente", e);
        }
    }

    private String altaNumPoliza(UsuarioAlta usuarioAlta,ObjectMapper mapper,
        String idCliente,StringBuilder clientName,StringBuilder datosServicio) {
        LOG.info(INI +getMethodName());
        try
        {
            Poliza poliza = new Poliza();
            poliza.setNumPoliza(Integer.valueOf(usuarioAlta.getNumPoliza()));
            poliza.setNumColectivo(Integer.valueOf(usuarioAlta.getNumDocAcreditativo()));
            poliza.setCompania(1);

            PolizaBasico polizaBasicoConsulta = new PolizaBasicoFromPolizaBuilder().withPoliza( poliza ).build();

            final util.datos.DetallePoliza detallePolizaResponse = portalclientesWebEJBRemote.recuperarDatosPoliza(polizaBasicoConsulta);

            clientName.append(detallePolizaResponse.getTomador().getNombre()).append(" ").
                        append(detallePolizaResponse.getTomador().getApellido1()).append(" ").
                        append(detallePolizaResponse.getTomador().getApellido2());

            idCliente = detallePolizaResponse.getTomador().getIdentificador();
            datosServicio.append("Datos recuperados del servicio de tarjeta:").append(ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(detallePolizaResponse));
        }catch(Exception e)
        {
            LOG.error("Error al obtener los datos de la poliza", e);
        }
        return idCliente;
    }

    private String altaNumtarjeta(UsuarioAlta usuarioAlta,ObjectMapper mapper,String idCliente,
        StringBuilder clientName,StringBuilder datosServicio) {
        LOG.info(INI +getMethodName());
        try{
            String urlToRead = TARJETAS_GETDATOS + usuarioAlta.getNumTarjeta();
            ResponseEntity<String> res = restTemplate.getForEntity( urlToRead, String.class);
            if(res.getStatusCode() == HttpStatus.OK){
                String dusuario = res.getBody();
                clientName.append(dusuario);
                idCliente = dusuario;
                datosServicio.append("Datos recuperados del servicio de tarjeta:").append(ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(dusuario));
            }
        }catch(Exception e)
        {
            LOG.error("Error al obtener los datos de la tarjeta", e);
        }
        return idCliente;
    }

    private List< ValueCode > getTiposDocumentosRegistro() {
        return Arrays.asList( new ValueCode(), new ValueCode() ); // simulacion servicio externo
    }

    /**
     * MÃ©todo para parsear el JSON de respuesta de los servicios de tarjeta/pÃ³liza
     *
     * @param resBravo
     * @return
     */
    private String parseJsonBravo(StringBuilder resBravo)
    {
        return resBravo.toString().replaceAll("[\\[\\]\\{\\}\\\"\\r]", "").replaceAll(ESCAPED_LINE_SEPARATOR, ESCAPE_ER + ESCAPED_LINE_SEPARATOR);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
