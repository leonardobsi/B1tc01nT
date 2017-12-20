package br.com.leeo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.Wallet;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import br.com.leeo.jtelegram.File;
import br.com.leeo.jtelegram.InlineKeyboardButton;
import br.com.leeo.jtelegram.InlineKeyboardMarkup;
import br.com.leeo.jtelegram.KeyboardButton;
import br.com.leeo.jtelegram.LabeledPrice;
import br.com.leeo.jtelegram.ReplyKeyboardMarkup;
import br.com.leeo.jtelegram.Update;

public class Main {

	// https://api.telegram.org/bot488308926:AAF2AnhPWxJD8-hLSqymWwKc8kD-LX2oKoI/getUpdates
	
	private static final long btcPrice= 70000;
	private static final float percent= 1.25F;

	public static void main(String[] args) {

		NetworkParameters params = MainNetParams.get();
		WalletAppKit kit = new WalletAppKit(params, new java.io.File("."), "sendrequest-main");
		kit.startAsync();
		kit.awaitRunning();

		System.out.println("It's ready");
		
		JTelegram jTelegram = new JTelegram("488308926:AAF2AnhPWxJD8-hLSqymWwKc8kD-LX2oKoI") {

			@Override
			public void handle(Update update) {

				if( update.getPre_checkout_query() != null ) {
					
					// If have balance... True.
			        Coin value = Coin.parseCoin( update.getPre_checkout_query().getInvoice_payload().split( "@@" )[1] );
			        
					answerPreCheckoutQuery( update.getPre_checkout_query(), kit.wallet().getBalance().value > value.value | true);
					
				}else if( update.getCallback_query() != null ) {
					
					answerCallbackQuery(update.getCallback_query(), "Working...", false);
					deleteMessage(update.getCallback_query().getMessage());
					
					List<LabeledPrice> prices= new ArrayList<>();
					try {
						
						// R$ 3,29	~ R$ 32.920,05
						if( "0.001".equals( update.getCallback_query().getData() ) ) {
							
							prices.add(new LabeledPrice( update.getCallback_query().getData(), (int) (btcPrice * 0.001 * percent) ));
							sendInvoice( update.getCallback_query().getMessage().getChat().getId(), "BitCoin", "0.001 BTC", update.getCallback_query().getMessage().getText() + "@@" + update.getCallback_query().getData(), "361519591:TEST:e3c1aa5d63ddf9a6fc4aec3fe449c894", "start_parameter", Currency.BRL, prices, new URL("https://en.bitcoin.it/w/images/en/5/5c/WeLv_BC_Badge_128px.png"));

						}else if( "0.0001".equals( update.getCallback_query().getData() ) ) {
							
							prices.add(new LabeledPrice( update.getCallback_query().getData(), (int) (btcPrice * 0.0001 * percent) ));
							sendInvoice( update.getCallback_query().getMessage().getChat().getId(), "BitCoin", "0.0001 BTC", update.getCallback_query().getMessage().getText() + "@@" + update.getCallback_query().getData(), "361519591:TEST:e3c1aa5d63ddf9a6fc4aec3fe449c894", "start_parameter", Currency.BRL, prices, new URL("https://en.bitcoin.it/w/images/en/5/5c/WeLv_BC_Badge_128px.png"));
						}

					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					
				}else if ("/start".equals(update.getMessage().getText())) {

					sendMessage(update.getMessage().getChat().getId(), kit.wallet().currentReceiveAddress().toString(), ParseMode.Markdown, new ReplyMarkup());
					
					ReplyKeyboardMarkup replyKeyboardMarkup= new ReplyKeyboardMarkup();

					List<KeyboardButton> keyboardButtons= new ArrayList<>();
					KeyboardButton keyboardButton= new KeyboardButton("Current account balance", false, false);
					keyboardButtons.add(keyboardButton);

					replyKeyboardMarkup.getKeyboard().add( keyboardButtons );
					
					sendMessage(update.getMessage().getChat().getId(), "Balance: " + kit.wallet().getBalance().toString()
							+ " BTC\n\n0.001 BTC - R$ " + (btcPrice * 0.001 * percent) + "\n0.0001 BTC - R$ " + (btcPrice * 0.0001 * percent) , ParseMode.Markdown, replyKeyboardMarkup);				
					
				}else if( update.getMessage().getSuccessful_payment() != null ) {
					
					try {
						
				        Coin value = Coin.parseCoin( update.getMessage().getSuccessful_payment().getInvoice_payload().split( "@@" )[1] );
				        Address to = Address.fromBase58(params, update.getMessage().getSuccessful_payment().getInvoice_payload().split( "@@" )[0]);

						Wallet.SendResult result = kit.wallet().sendCoins(kit.peerGroup(), to, value);
						
						sendMessage(update.getMessage().getChat().getId(), "BitCoins sent. Transaction hash: " + result.tx.getHashAsString(), ParseMode.Markdown, new ReplyMarkup());

					} catch (InsufficientMoneyException e) {

						sendMessage(update.getMessage().getChat().getId(), "Not enough balance in wallet.", ParseMode.Markdown, new ReplyMarkup());
					}
					
				}else if (update.getMessage().getPhoto() != null) {

					File file = getFile(update.getMessage().getPhoto().get(2).getFile_id());

					try {

						BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(new URL("https://api.telegram.org/file/bot<token>/<file_path>"
								.replaceAll("<token>", "488308926:AAF2AnhPWxJD8-hLSqymWwKc8kD-LX2oKoI").replaceAll("<file_path>", file.getFile_path()))))));
						Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
						
				        Address to = Address.fromBase58(params, qrCodeResult.getText().replaceAll("^bitcoin:", ""));

						InlineKeyboardMarkup inlineKeyboardMarkup= new InlineKeyboardMarkup();
						
						List<InlineKeyboardButton> inlineKeyboardButtons= new ArrayList<>();
						// inlineKeyboardButtons.add(new InlineKeyboardButton("0.01 BTC", "0.01"));
						inlineKeyboardButtons.add(new InlineKeyboardButton("0.001 BTC", "0.001"));
						inlineKeyboardButtons.add(new InlineKeyboardButton("0.0001 BTC", "0.0001"));
						
						inlineKeyboardMarkup.getInline_keyboard().add( inlineKeyboardButtons );
						
						sendMessage(update.getMessage().getChat().getId(), to.toString(), ParseMode.Markdown, inlineKeyboardMarkup);

					} catch (Exception e) {

						sendMessage(update.getMessage().getChat().getId(), "Try again, invalid wallet address.", ParseMode.Markdown, new ReplyMarkup());
					}
				}else if( "Current account balance".equals( update.getMessage().getText() ) ){
					
					sendMessage(update.getMessage().getChat().getId(), "Balance: " + kit.wallet().getBalance().toString()
							+ " BTC\n\n0.001 BTC - R$ " + (btcPrice * 0.001 * percent) + "\n0.0001 BTC - R$ " + (btcPrice * 0.0001 * percent) , ParseMode.Markdown, new ReplyMarkup());				
					
				}else {
					
			        try {
						Address to = Address.fromBase58(params, update.getMessage().getText());

						InlineKeyboardMarkup inlineKeyboardMarkup= new InlineKeyboardMarkup();
						
						List<InlineKeyboardButton> inlineKeyboardButtons= new ArrayList<>();
						inlineKeyboardButtons.add(new InlineKeyboardButton("0.0001 BTC", "0.0001"));
						inlineKeyboardButtons.add(new InlineKeyboardButton("0.001 BTC", "0.001"));
						
						inlineKeyboardMarkup.getInline_keyboard().add( inlineKeyboardButtons );
						
						sendMessage(update.getMessage().getChat().getId(), to.toString(), ParseMode.Markdown, inlineKeyboardMarkup);
						
					} catch (AddressFormatException e) {
						
						sendMessage(update.getMessage().getChat().getId(), "Try again, invalid wallet address.", ParseMode.Markdown, new ReplyMarkup());
					}
				}
			}
		};

		while (true) {

			jTelegram.loadUpdates();
			
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
