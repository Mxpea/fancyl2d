import { Application, Ticker } from 'pixi.js';

async function start() {
  if (!globalThis.Live2DCubismCore) throw new Error('Cubism Core did not load');
  const { Live2DModel } = await import('pixi-live2d-display/cubism4');
  const config = JSON.parse(new URLSearchParams(location.search).get('config'));
  Live2DModel.registerTicker(Ticker);
  const app = new Application({ view: document.querySelector('#model'),
    backgroundAlpha: 0, antialias: true, autoDensity: false, resolution: 1,
    width: innerWidth, height: innerHeight });
  app.ticker.maxFPS = config.fps;
  Ticker.shared.maxFPS = config.fps;
  const model = await Live2DModel.from(new URL(config.model, location.href).href, {
    autoInteract: false, idleMotionGroup: config.idle ? 'Idle' : '__fancyl2d_no_idle__',
  });
  if (!config.idle) {
    // The pinned Cubism adapter treats both helpers as optional; keep physics for cursor-driven motion.
    model.internalModel.eyeBlink = undefined;
    model.internalModel.breath = undefined;
  }
  if (config.sway > 0) {
    const started = performance.now();
    app.ticker.add(() => {
      model.rotation = Math.sin((performance.now() - started) * Math.PI / 2500) * config.sway * Math.PI / 45;
    });
  }
  const width = model.internalModel.width;
  const height = model.internalModel.height;
  if (!(width > 0 && height > 0)) throw new Error('Invalid model canvas');
  function fit() {
    app.renderer.resize(innerWidth, innerHeight);
    model.position.set(innerWidth / 2, innerHeight / 2);
    model.scale.set(Math.min(innerWidth / width, innerHeight / height) * config.scale);
  }
  model.anchor.set(0.5);
  app.stage.addChild(model);
  fit();
  addEventListener('resize', fit);
  window.fancyL2dFocus = (x, y) => {
    model.internalModel.focusController.focus(x * config.follow, y * config.follow);
  };
  window.fancyL2dAnimate = running => {
    if (running) {
      Ticker.shared.start();
      app.start();
      app.render();
    } else {
      app.stop();
      Ticker.shared.stop();
    }
  };
  app.view.addEventListener('webglcontextlost', () => report('error', 'WebGL context lost'));
  app.render();
  await report('ready');
}

function report(state, body) {
  return fetch(`./${state}`, { method: 'POST', body }).catch(console.error);
}

start().catch(error => {
  console.error('[FancyL2D]', error);
  report('error', error?.stack ?? String(error));
});
