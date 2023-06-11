package com.gmail.renanfr1047;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.gmail.renanfr1047.model.RegistroEvento;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class EventRegistrationBot {
    private static final String DESIRED_TYPE_OF_REGISTRATION = "Inscrição Free - 10ª Edição";
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private static final int DELAY_MS = 500;

    private WebDriver driver;
    private WebDriverWait wait;

    public EventRegistrationBot() {
        System.setProperty("webdriver.chrome.driver", EventRegistrationBot.class.getResource("/chromedriver.exe").getPath());
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, WAIT_TIMEOUT_SECONDS);
    }

    public void run() {
        try {
            navigateToEventPage();

            if (isEventRegistrationAvailable()) {
                incrementRegistrationAmount();
                selectRegistrationType();
                fillRegistrationForm();
                confirmRegistration();
                processConfirmationPage();
            } else {
                System.out.println("Tipo de inscrição não está mais disponível.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private void navigateToEventPage() {
        driver.get("https://www.sympla.com.br/evento-online/congresso-de-ti-10-edicao/1927921");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[data-testid='ticket-grid']")));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState"));
    }

    private boolean isEventRegistrationAvailable() {
        Document document = Jsoup.parse(driver.getPageSource());
        Elements ticketItems = document.select("div[data-testid='ticket-grid-item']");
        for (Element item : ticketItems) {
            String titleOfRegistrationType = item.getElementsByTag("span").get(0).text();
            if (titleOfRegistrationType.equals(DESIRED_TYPE_OF_REGISTRATION)) {
                String buttonText = item.select("button[aria-label='Increase Amount']").text();
                return !buttonText.equalsIgnoreCase("Encerrado");
            }
        }
        return false;
    }

    private void incrementRegistrationAmount() {
        WebElement addButton = getAddButtonForRegistrationType(DESIRED_TYPE_OF_REGISTRATION);
        moveToElement(addButton);
        addButton.click();
    }

    private WebElement getAddButtonForRegistrationType(String registrationType) {
        WebElement spanElement = driver.findElement(By.xpath("//span[text()='" + registrationType + "']"));
        return wait.until(ExpectedConditions.elementToBeClickable(spanElement.findElement(By.xpath("./../..")).findElement(
                By.xpath("./following-sibling::div//button[@aria-label='Increase Amount']"))));
    }

    private void selectRegistrationType() {
        WebElement selectButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Realizar Inscrição')]")));
        selectButton.click();
    }

    private void fillRegistrationForm() {
        Random random = new Random();
        String nome = "Exemplo" + random.nextInt(1000);
        String sobrenome = "de Teste" + random.nextInt(1000);
        String email = "exemplo" + random.nextInt(1000) + "@teste.com";
        String whatsapp = "1234567890";
        String empresa = "Empresa Exemplo" + random.nextInt(1000);
        String cargo = "Cargo Exemplo" + random.nextInt(1000);
        String inscricao = "Inscrição nº1";

        RegistroEvento registro = new RegistroEvento(nome, sobrenome, email, whatsapp, empresa, cargo, inscricao);

        fillInputField("Nome", registro.nome());
        fillInputField("Sobrenome", registro.sobrenome());
        fillInputField("E-mail", registro.email());
        fillInputField("Whatsapp (Opcional)", registro.whatsapp());
        fillInputField("Empresa em que trabalha", registro.empresa());
        fillInputField("Cargo", registro.cargo());

        WebElement termosCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//input[@type='checkbox'])[1]")));
        moveToElement(termosCheckbox);
        termosCheckbox.click();

        WebElement politicaCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//input[@type='checkbox'])[2]")));
        moveToElement(politicaCheckbox);
        politicaCheckbox.click();

        WebElement inscricaoSelect = driver.findElement(By.xpath("//select[@name='selectedParticipant']"));
        inscricaoSelect.sendKeys(registro.inscricao());

        fillInputField("Confirmação do e-mail", registro.email());
    }

    private void fillInputField(String label, String value) {
        WebElement labelElement = driver.findElement(By.xpath("//label[contains(.,'" + label + "')]"));
        WebElement inputElement = labelElement.findElement(By.xpath("./../following-sibling::div//input"));
        moveToElement(inputElement);
        inputElement.sendKeys(value);
    }

    private void confirmRegistration() {
        WebElement finalizarButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Finalizar')]")));
        moveToElement(finalizarButton);
        finalizarButton.click();
    }

    private void processConfirmationPage() {
        wait.until(ExpectedConditions.urlContains("sucesso"));
        Document confirmationPage = Jsoup.parse(driver.getPageSource());
        Element successMessage = confirmationPage.selectFirst("h2:contains(Pedido efetuado com sucesso!)");
        if (successMessage != null) {
            System.out.println("Pedido efetuado com sucesso!");
            Element numeroPedidoElement = confirmationPage.selectFirst("p:contains(nº do pedido)");
            if (numeroPedidoElement != null) {
                Element proximoElementoP = numeroPedidoElement.nextElementSibling();
                String textoProximoP = proximoElementoP.text();
                System.out.println("Texto do próximo elemento <p> após 'nº do pedido': " + textoProximoP);
            } else {
                System.out.println("Elemento 'nº do pedido' não encontrado na página de confirmação.");
            }
        } else {
            System.out.println("Falha ao efetuar o pedido.");
        }
    }

    private void moveToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        delay(DELAY_MS);
        Actions actions = new Actions(driver);
        actions.moveToElement(element).perform();
        delay(DELAY_MS);
    }

    private void delay(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        EventRegistrationBot bot = new EventRegistrationBot();
        bot.run();
    }
}
