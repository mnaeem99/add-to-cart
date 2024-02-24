package com.selver.addtocart;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
public class SelverBasketAPIController {
    private final Map<String, String> users = new HashMap<>();

    @PostMapping("/selver-basket-api")
    public Map<String, String> processMultipleProducts(@RequestBody List<Product> products) throws Exception {
        String uuid = UUID.randomUUID().toString();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("start-maximized");
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions");
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 640 XL LTE) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Mobile Safari/537.36 Edge/12.10166");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.get("https://www.selver.ee/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.xpath("//iframe[@title='Widget containing a Cloudflare security challenge']")));
            // Wait for CAPTCHA checkbox to be clickable and click it
            WebElement captchaCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@class='ctp-checkbox-label']")));
            captchaCheckbox.click();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            WebElement acceptBtn = driver.findElement(By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll"));
            acceptBtn.click();
        }catch (Exception e){
            e.printStackTrace();
        }

        Thread.sleep(2000);

        try {
            for (Product product: products) {
                driver.navigate().to("https://www.selver.ee/"+product.getName());
                WebElement addToCartButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-testid='addToCart']")));
                if (product.getQuantity() == 1) {
                    addToCartButton.click();
                } else {
                    for (int i = 0; i < product.getQuantity(); i++) {
                        addToCartButton.click();
                        Thread.sleep(1000);
                    }
                }
                Thread.sleep(1000);
            }

            Thread.sleep(2000);

            // Navigate to the cart URL
            driver.navigate().to("https://www.selver.ee/cart");

            // Wait for the "Share Cart" button to be clickable and click it
            WebElement shareCartButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ShareCart__popup-trigger.Button")));
            shareCartButton.click();

            Thread.sleep(2000);

            WebElement shareLinkInput = driver.findElement(By.cssSelector("[name='share-url']"));
            String basketUrl = shareLinkInput.getAttribute("value");

            System.out.println(basketUrl);

            driver.quit();

            users.put(uuid, basketUrl);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Map.of("userSession", uuid);
    }
    @GetMapping("/selver-basket-api/userSession/{userSession}")
    public Map<String, String> getUserSession(@PathVariable String userSession) {
        String url = users.get(userSession);
        if (url != null) {
            return Map.of("source", "SELVER", "name", "BASKET_URL", "userSession", userSession, "url", url);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User session not found");
        }
    }


}


