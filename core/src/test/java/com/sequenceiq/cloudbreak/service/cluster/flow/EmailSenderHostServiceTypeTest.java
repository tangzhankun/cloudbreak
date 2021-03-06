package com.sequenceiq.cloudbreak.service.cluster.flow;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.io.CharStreams;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class EmailSenderHostServiceTypeTest {
    private static final String NAME_OF_THE_CLUSTER = "name-of-the-cluster";

    private GreenMail greenMail;

    @Mock
    private CachedUserDetailsService cachedUserDetailsService;

    @InjectMocks
    private final EmailSenderService emailSenderService = new EmailSenderService();

    @Mock
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Before
    public void before() throws IOException, TemplateException {
        greenMail = new GreenMail(new ServerSetup(3465, null, ServerSetup.PROTOCOL_SMTP));
        greenMail.setUser("demouser", "demopwd");
        greenMail.start();

        IdentityUser identityUser = new IdentityUser("sdf", "testuser", "testaccount", new ArrayList<>(), "familyname", "givenName", new Date());
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration());

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/cluster-installer-mail-success.ftl");
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/cluster-installer-mail-fail.ftl");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(3465);
        mailSender.setUsername("demouser2");
        mailSender.setPassword("demopwd2");

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", false);
        props.put("mail.smtp.starttls.enable", false);
        props.put("mail.debug", false);

        mailSender.setJavaMailProperties(props);

        ReflectionTestUtils.setField(emailSenderService, "mailSender", mailSender);

        EmailMimeMessagePreparator mmp = new EmailMimeMessagePreparator();
        ReflectionTestUtils.setField(mmp, "msgFrom", "no-reply@hortonworks.com");

        ReflectionTestUtils.setField(emailSenderService, "emailMimeMessagePreparator", mmp);

        when(cachedUserDetailsService.getDetails(anyString(), any(UserFilterField.class)))
                .thenReturn(identityUser);

    }

    @After
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    public void testSendTerminationSuccessEmail() throws IOException, MessagingException {
        // GIVEN
        String subject = "Your cluster has been terminated";
        // WHEN
        emailSenderService.sendTerminationSuccessEmail("xxx", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
        // THEN
        greenMail.waitForIncomingEmail(5000L, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1L, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("successfully terminated"));
    }

    @Test
    public void testSendTerminationFailureEmail() throws IOException, MessagingException {
        // GIVEN
        String subject = "Cluster termination failed";
        //WHEN
        emailSenderService.sendTerminationFailureEmail("xxx", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
        //THEN
        greenMail.waitForIncomingEmail(5000L, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1L, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Failed to terminate your cluster"));
    }

    @Test
    public void testSendProvisioningFailureEmail() throws IOException, MessagingException {
        //GIVEN
        String subject = "Cluster install failed";
        //WHEN
        emailSenderService.sendProvisioningFailureEmail("xxx", "xxx", NAME_OF_THE_CLUSTER);
        //THEN
        greenMail.waitForIncomingEmail(5000L, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1L, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Something went terribly wrong"));
    }

    @Ignore
    @Test
    public void testSendProvisioningSuccessEmailSMTPS() throws IOException, MessagingException {
        //To run this test, please download the greenmail.jks from the following link:
        // https://github.com/greenmail-mail-test/greenmail/blob/master/greenmail-core/src/main/resources/greenmail.jks
        // and put into the /cert/trusted directory, and set the mail.transport.protocol variable to smtps.
        greenMail = new GreenMail(ServerSetupTest.SMTPS);
        greenMail.start();
        //GIVEN
        String content = getFileContent().replaceAll("\\n", "");
        String subject = String.format("%s cluster installation", NAME_OF_THE_CLUSTER);
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("test@example.com", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER, false);

        //THEN
        greenMail.waitForIncomingEmail(5000L, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1L, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertTrue(String.valueOf(messages[0].getContent()).replaceAll("\\n", "").replaceAll("\\r", "").contains(content));
    }

    @Test
    public void testSendProvisioningSuccessEmailSmtp() throws IOException, MessagingException {
        //GIVEN
        String subject = "Your cluster is ready";
        emailSenderService.sendProvisioningSuccessEmail("xxx@alma.com", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER, false);

        //THEN
        greenMail.waitForIncomingEmail(5000L, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1L, messages.length);
        Assert.assertEquals("Your cluster '" + NAME_OF_THE_CLUSTER + "' is ready", messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Your cluster '" + NAME_OF_THE_CLUSTER + "' is ready"));
    }

    Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    private String getFileContent() throws IOException {
        return CharStreams.toString(new InputStreamReader(new ClassPathResource("mail/cluster-installer-mail-success").getInputStream()));
    }

}