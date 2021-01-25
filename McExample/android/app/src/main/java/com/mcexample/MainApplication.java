package com.mcexample;

import android.app.Application;
import android.content.Context;
import com.facebook.react.PackageList;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.soloader.SoLoader;
import com.mastercard.mchipengine.walletinterface.walletcommonenumeration.CvmModel;
import com.mastercard.mpsdk.walletusabilitylayer.api.Wul;
import com.mastercard.mpsdk.walletusabilitylayer.config.TransactionPolicy;
import com.mastercard.mpsdk.walletusabilitylayer.config.UserAuthMode;
import com.mastercard.mpsdk.walletusabilitylayer.config.WalletConfiguration;
import com.mastercard.mpsdksample.androidcryptoengine.McbpCryptoEngineFactory;
import com.mastercard.mpsdksample.mpausingwul.receiver.InputValidationServiceImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;


public class MainApplication extends Application implements ReactApplication {

  private final ReactNativeHost mReactNativeHost =
      new ReactNativeHost(this) {
        @Override
        public boolean getUseDeveloperSupport() {
          return BuildConfig.DEBUG;
        }

        @Override
        protected List<ReactPackage> getPackages() {
          List<ReactPackage> packages = new PackageList(this).getPackages();
          packages.add(new MyAppPackage());
          return packages;
        }

        @Override
        protected String getJSMainModuleName() {
          return "index";
        }
      };



    public void initMpSdk() {

        // create policies

        TransactionPolicy lvtPolicy = new TransactionPolicy(TransactionPolicy.Type.LVT)
                .allowTransactions(true)
                .numberOfTransactionsAllowedBeforeUnlock(0)
                .numberOfTransactionsAllowedBeforeAuth(0)
                .secondsAllowedSinceLastAuth(0);

        TransactionPolicy hvtPolicy = new TransactionPolicy(TransactionPolicy.Type.HVT)
                .allowTransactions(true)
                .numberOfTransactionsAllowedBeforeUnlock(0)
                .numberOfTransactionsAllowedBeforeAuth(0)
                .secondsAllowedSinceLastAuth(0);

        TransactionPolicy transitPolicy = new TransactionPolicy(TransactionPolicy.Type.TRANSIT)
                .allowTransactions(false);

        TransactionPolicy unknownPolicy = new TransactionPolicy(TransactionPolicy.Type.UNKNOWN)
                .copyOf(hvtPolicy);

        // create configuration

        WalletConfiguration.Builder builder = new WalletConfiguration.Builder(this)

                .withCryptoEngine(
                        new McbpCryptoEngineFactory().
                                getCryptoEngine(this, new InputValidationServiceImpl(this)))
                .withUserAuthMode(UserAuthMode.CARD_PIN)
                .withCvmModel(CvmModel.CDCVM_ALWAYS)
                .withPolicy(lvtPolicy)
                .withPolicy(hvtPolicy)
                .withPolicy(transitPolicy)
                .withPolicy(unknownPolicy);

        WalletConfiguration conf = builder.build();
        Wul.init(this, conf);
    }

    @Override
  public ReactNativeHost getReactNativeHost() {
    return mReactNativeHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, /* native exopackage */ false);
    initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
    initMpSdk();
  }

  /**
   * Loads Flipper in React Native templates. Call this in the onCreate method with something like
   * initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
   *
   * @param context
   * @param reactInstanceManager
   */
  private static void initializeFlipper(
      Context context, ReactInstanceManager reactInstanceManager) {
    if (BuildConfig.DEBUG) {
      try {
        /*
         We use reflection here to pick up the class that initializes Flipper,
        since Flipper library is not available in release mode
        */
        Class<?> aClass = Class.forName("com.mcexample.ReactNativeFlipper");
        aClass
            .getMethod("initializeFlipper", Context.class, ReactInstanceManager.class)
            .invoke(null, context, reactInstanceManager);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }
}
