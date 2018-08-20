package org.aksw.limes.core.gui.controller.ml;

import org.aksw.limes.core.gui.controller.MainController;
import org.aksw.limes.core.gui.controller.TaskProgressController;
import org.aksw.limes.core.gui.model.ActiveLearningResult;
import org.aksw.limes.core.gui.model.Config;
import org.aksw.limes.core.gui.model.ml.ActiveLearningModel;
import org.aksw.limes.core.gui.view.TaskProgressView;
import org.aksw.limes.core.gui.view.ml.ActiveLearningResultView;
import org.aksw.limes.core.gui.view.ml.MachineLearningView;
import org.aksw.limes.core.io.cache.ACache;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * This class handles the interaction between the {@link MachineLearningView}
 * and the {@link ActiveLearningModel} according to the MVC Pattern for the
 * supervised active learning
 *
 * @author Daniel Obraczka {@literal <} soz11ffe{@literal @}
 *         studserv.uni-leipzig.de{@literal >}
 *
 */
public class ActiveLearningController extends MachineLearningController {

	private final MainController mainController;

	/**
	 * Constructor creates the according {@link ActiveLearningModel}
	 *
	 * @param config
	 *            containing information
	 * @param sourceCache
	 *            source information
	 * @param targetCache
	 *            target information
	 * @param mainController
	 *            mainController
	 */
	public ActiveLearningController(Config config, ACache sourceCache, ACache targetCache,
			MainController mainController) {
		this.mainController = mainController;
		this.mlModel = new ActiveLearningModel(config, sourceCache, targetCache);
	}

	/**
	 * Creates a learning task and launches a {@link TaskProgressView}. The
	 * results are shown in a {@link ActiveLearningResultView}
	 *
	 * @param view
	 *            MachineLearningView to manipulate elements in it
	 */
	@Override
	public void learn(MachineLearningView view) {
		final Task<Void> learnTask = this.mlModel.createLearningTask();

		final TaskProgressView taskProgressView = new TaskProgressView("Learning");
		final TaskProgressController taskProgressController = new TaskProgressController(taskProgressView);
		taskProgressView.getCancelled().addListener(
				(ChangeListener<Boolean>) (observable, oldValue, newValue) -> view.getLearnButton().setDisable(false));
		taskProgressController.addTask(learnTask, items -> {
			view.getLearnButton().setDisable(false);
			final ObservableList<ActiveLearningResult> results = FXCollections.observableArrayList();
			((ActiveLearningModel) this.mlModel).getNextExamples().getMap().forEach((sourceURI, map2) -> {
				map2.forEach((targetURI, value) -> {
					results.add(new ActiveLearningResult(sourceURI, targetURI, value));
				});
			});
			Platform.runLater(() -> {
				final ActiveLearningResultView activeLearningResultView = new ActiveLearningResultView(
						ActiveLearningController.this.mlModel.getConfig(),
						(ActiveLearningModel) ActiveLearningController.this.mlModel,
						ActiveLearningController.this.mainController);
				activeLearningResultView.showResults(results);
			});
			// TODO error handling);
		}, error -> {
			view.showErrorDialog("Error during learning", error.getMessage());
		});

	}

}
